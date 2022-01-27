/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.taxfraudreporting.services

import akka.actor.ActorSystem
import akka.stream.SourceShape
import akka.stream.scaladsl.{Broadcast, Concat, GraphDSL, Source}
import akka.util.ByteString
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}
import uk.gov.hmrc.objectstore.client.Path
import uk.gov.hmrc.objectstore.client.play.Implicits.{akkaSourceContentWrite, futureMonad}
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.taxfraudreporting.models.FraudReport
import uk.gov.hmrc.taxfraudreporting.repositories.FraudReportRepository

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, LocalTime, ZoneOffset}
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationInt, DurationLong}
import scala.language.postfixOps

@Singleton
class FraudReportStorageWorker @Inject() (
  xmlElementFactory: XmlElementFactory,
  fraudReportRepository: FraudReportRepository,
  lockRepository: MongoLockRepository,
  objectStoreClient: PlayObjectStoreClient,
  servicesConfig: ServicesConfig
)(implicit headerCarrier: HeaderCarrier, executionContext: ExecutionContext, actorSystem: ActorSystem) {
  private val logger = Logger(this.getClass)

  private def configured(prop: String) = servicesConfig getString s"worker.$prop"

  private val path           = Path Directory configured("path")
  private val fileNameFormat = configured("file.nameFormat")
  private val xmlTag         = configured("file.rootElement")
  private val interval       = 86400 / configured("frequency").toInt
  private val timeOfJob      = LocalTime parse configured("timeOfJob")

  private val jobEpoch = LocalDate.now() atTime timeOfJob toEpochSecond ZoneOffset.UTC
  private val nowEpoch = LocalDateTime.now() toEpochSecond ZoneOffset.UTC

  private val timeDiffs = Stream.iterate(jobEpoch - nowEpoch)(_ + interval)
  private val delay     = timeDiffs dropWhile { _ <= 0 } head

  private def contentSource(extractTime: LocalDateTime) = Source fromGraph GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val bCast  = builder add Broadcast[FraudReport](2)
    val concat = builder add Concat[String](4)

    Source.single(s"""<?xml version="1.0" encoding="UTF-8"?><$xmlTag>""") ~> concat.in(0)

    Source.fromPublisher(fraudReportRepository.listUnsent) ~> bCast.in

    bCast.out(0)
      .fold(0)((acc, _) => acc + 1)
      .map(xmlElementFactory.getFileHeader(extractTime, _).toString) ~> concat.in(1)

    bCast.out(1)
      .zipWithIndex
      .map(xmlElementFactory.getReport(_).toString) ~> concat.in(2)

    Source.single(s"</$xmlTag>") ~> concat.in(3)

    SourceShape(concat.out)
  } map { ByteString(_) }

  logger.info(s"Initialising worker. First job in $delay seconds.")

  actorSystem.scheduler.scheduleAtFixedRate(delay seconds, interval seconds) { () =>
    logger.info("Commencing job.")

    LockService(lockRepository, "ris-kana-lock", ttl = 1.hour) withLock {
      val extractTime = LocalDateTime.now()
      val fileName    = fileNameFormat.format(extractTime.format(DateTimeFormatter ofPattern "yyyyMMddHHmmss"))

      objectStoreClient.putObject(path file fileName, contentSource(extractTime))
    } foreach {
      case Some(summary) => logger.info(s"Stored object: ${summary.contentMd5}.")
      case None          => logger.info("Lock not acquired.")
    }
  }
}
