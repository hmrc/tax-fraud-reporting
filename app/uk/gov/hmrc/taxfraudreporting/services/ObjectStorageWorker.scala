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
import akka.stream.scaladsl.{Keep, Sink, SinkQueueWithCancel, Source}
import play.api.{Configuration, Logger}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.MongoLockRepository
import uk.gov.hmrc.objectstore.client.play.Implicits.{akkaSourceContentWrite, futureMonad}
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient
import uk.gov.hmrc.objectstore.client.{ObjectSummaryWithMd5, Path}

import java.time.{LocalDate, LocalDateTime, LocalTime, ZoneOffset}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{DurationInt, DurationLong}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

@Singleton
class ObjectStorageWorker @Inject() (
  val configuration: Configuration,
  fraudReportStreamer: FraudReportStreamer,
  lockRepository: MongoLockRepository,
  objectStoreClient: PlayObjectStoreClient
)(implicit executionContext: ExecutionContext, actorSystem: ActorSystem)
    extends Configured("objectStorageWorker") {
  private val logger = Logger(getClass)

  implicit private val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val path = Path Directory configured("path")

  private val daySecs = 86400

  private val interval  = daySecs / configured("frequency").toInt
  private val timeOfJob = LocalTime parse configured("timeOfJob")

  private val jobEpoch = LocalDate.now() atTime timeOfJob toEpochSecond ZoneOffset.UTC
  private val nowEpoch = LocalDateTime.now() toEpochSecond ZoneOffset.UTC
  val delay: Long      = (jobEpoch - nowEpoch + daySecs) % interval

  logger.info(s"First job in $delay s to repeat every $interval s.")

  def storeObject(correlationID: UUID): Future[ObjectSummaryWithMd5] = {
    val extractTime  = LocalDateTime.now()
    val fraudReports = fraudReportStreamer.stream(correlationID, extractTime)
    val fileName     = getFileName(extractTime)

    logger.info(s"Storing object $fileName.")

    objectStoreClient.putObject(path file fileName, fraudReports)
  }

  private val lockID = "lockID"
  private val owner  = "owner"

  def job: Future[Option[ObjectSummaryWithMd5]] = {
    logger.info("Commencing job.")

    lockRepository.isLocked(lockID, owner) flatMap {
      isLocked =>
        if (isLocked) {
          logger.info("Job already locked; leaving to other worker.")
          Future(None)
        } else
          lockRepository.takeLock(lockID, owner, 1.hours) flatMap {
            gainedLock =>
              if (gainedLock) {
                logger.info("Lock taken successfully.")
                val correlationID = UUID.randomUUID()
                storeObject(correlationID) map {
                  // TODO: SDES notification logic here
                  Some.apply
                }
              } else {
                logger.info("Error occurred trying to take lock.")
                Future(None)
              }
          }
    }
  }

  private def logResult(summaryOpt: Option[ObjectSummaryWithMd5]): Unit =
    summaryOpt match {
      case Some(summary) => logger.info(s"Stored object: ${summary.contentMd5}.")
      case None          => logger.info("Lock not acquired.")
    }

  val queue: SinkQueueWithCancel[Option[ObjectSummaryWithMd5]] =
    Source.tick(delay seconds, interval seconds, job)
      .flatMapConcat(Source.future)
      .wireTapMat(Sink.queue())(Keep.right)
      .toMat(Sink foreach logResult)(Keep.left)
      .run()

}
