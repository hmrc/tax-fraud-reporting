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
import uk.gov.hmrc.taxfraudreporting.models.sdes.{FileAudit, FileChecksum, FileMetaData, SDESFileNotifyRequest}
import uk.gov.hmrc.taxfraudreporting.repositories.FraudReportRepository

import java.time.{LocalDate, LocalDateTime, LocalTime, ZoneOffset}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{DurationInt, DurationLong}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

@Singleton
class ObjectStorageWorker @Inject() (
  val configuration: Configuration,
  fraudReportStreamer: FraudReportStreamer,
  lockRepository: MongoLockRepository,
  objectStoreClient: PlayObjectStoreClient,
  sdesService: SDESService,
  fraudReportRepository: FraudReportRepository
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

  private val recipientOrSender: String =
    configuration.get[String]("services.sdes.recipient-or-sender")

  private val informationType: String =
    configuration.get[String]("services.sdes.information-type")

  private val fileLocationUrl: String =
    configuration.get[String]("services.sdes.file-location-url")

  logger.info(s"First job in $delay s to repeat every $interval s.")

  def storeObject(correlationID: UUID, extractTime: LocalDateTime, fileName: String): Future[ObjectSummaryWithMd5] = {
    val fraudReports = fraudReportStreamer.stream(correlationID, extractTime)

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
                val extractTime   = LocalDateTime.now()
                val fileName      = getFileName(extractTime)
                storeObject(correlationID, extractTime, fileName) map { objWithSummary =>
                  notifySDES(correlationID, fileName, objWithSummary)
                  Some(objWithSummary)
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

  private def notifySDES(correlationID: UUID, fileName: String, objWithSummary: ObjectSummaryWithMd5): Unit = {
    val notifyRequest = createNotifyRequest(objWithSummary, fileName, correlationID)
    sdesService.fileNotify(notifyRequest).onComplete {
      case Success(_) =>
        logger.info(s"SDES has been notified of file :: ${fileName}  with correlationId::$correlationID")
        fraudReportRepository.updateUnprocessed(correlationID)
      case Failure(exception) =>
        logger.error(s"Error in notifying SDES about file :: $fileName correlationId:: $correlationID.", exception)
    }
  }

  private def createNotifyRequest(
    objSummary: ObjectSummaryWithMd5,
    fileName: String,
    uuid: UUID
  ): SDESFileNotifyRequest =
    SDESFileNotifyRequest(
      informationType,
      FileMetaData(
        recipientOrSender,
        fileName,
        s"$fileLocationUrl${objSummary.location.asUri}",
        FileChecksum(value = objSummary.contentMd5.value),
        objSummary.contentLength,
        List()
      ),
      FileAudit(uuid.toString)
    )

}
