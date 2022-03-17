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
import akka.stream.{ActorAttributes, Supervision}
import org.mongodb.scala.result.UpdateResult
import play.api.{Configuration, Logging}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.MongoLockRepository
import uk.gov.hmrc.objectstore.client.play.Implicits.{akkaSourceContentWrite, futureMonad}
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient
import uk.gov.hmrc.objectstore.client.{ObjectSummaryWithMd5, Path}
import uk.gov.hmrc.taxfraudreporting.models.sdes.{FileAudit, FileChecksum, FileMetaData, SDESFileNotifyRequest}
import uk.gov.hmrc.taxfraudreporting.repositories.FraudReportRepository

import java.time.{LocalDate, LocalDateTime, LocalTime, ZoneOffset}
import java.util.{Base64, UUID}
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{Duration, DurationInt, DurationLong}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.control.NonFatal

@Singleton
class ObjectStorageWorker @Inject() (
  val configuration: Configuration,
  fraudReportStreamer: FraudReportStreamer,
  lockRepository: MongoLockRepository,
  objectStoreClient: PlayObjectStoreClient,
  sdesService: SDESService,
  fraudReportRepository: FraudReportRepository
)(implicit executionContext: ExecutionContext, actorSystem: ActorSystem)
    extends Configured("objectStorageWorker") with Logging {

  private val supervisionStrategy: Supervision.Decider = {
    case NonFatal(e) =>
      logger.error("Object storage worker failed", e)
      Supervision.resume
    case e =>
      logger.error("Object storage worker failed", e)
      Supervision.stop
  }

  implicit private val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val path = Path Directory configured("path")

  private val daySecs = 86400

  private val interval  = daySecs / configured("frequency").toInt
  private val timeOfJob = configured.get("timeOfJob").map(LocalTime.parse).getOrElse(LocalTime.now)

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
  private val lockDuration: Duration = configuration.get[Duration]("services.objectStorageWorker.lock-duration")

  def job: Future[Option[ObjectSummaryWithMd5]] = {
    logger.info("Commencing job.")

    lockRepository.isLocked(lockID, owner) flatMap {
      isLocked =>
        if (isLocked) {
          logger.info("Job already locked; leaving to other worker.")
          Future(None)
        } else
          lockRepository.takeLock(lockID, owner, lockDuration) flatMap {
            gainedLock =>
              if (gainedLock) {
                logger.info("Lock taken successfully.")
                val correlationID = UUID.randomUUID()
                val extractTime   = LocalDateTime.now()
                val fileName      = getFileName(extractTime)
                for {
                  summary <- storeObject(correlationID, extractTime, fileName)
                  _       <- notifySDES(correlationID, fileName, summary)
                  _       <- lockRepository.releaseLock(lockID, owner)
                } yield Some(summary)
              } else {
                logger.error("Error occurred trying to take lock.")
                Future(None)
              }
          }
    }
  }

  val tap: SinkQueueWithCancel[Option[ObjectSummaryWithMd5]] =
    Source
      .tick(delay seconds, interval seconds, ())
      .mapAsync(1)(_ => job)
      .wireTapMat(Sink.queue())(Keep.right)
      .toMat(Sink.foreach(logResult))(Keep.left)
      .withAttributes(ActorAttributes.supervisionStrategy(supervisionStrategy))
      .run()

  private def notifySDES(
    correlationID: UUID,
    fileName: String,
    objWithSummary: ObjectSummaryWithMd5
  ): Future[UpdateResult] = {
    val notifyRequest = createNotifyRequest(objWithSummary, fileName, correlationID)
    sdesService.fileNotify(notifyRequest).flatMap(_ => fraudReportRepository.updateUnprocessed(correlationID))
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
        FileChecksum(value = Base64.getDecoder.decode(objSummary.contentMd5.value).map("%02x".format(_)).mkString),
        objSummary.contentLength,
        List()
      ),
      FileAudit(uuid.toString)
    )

  private def logResult(summaryOpt: Option[ObjectSummaryWithMd5]): Unit =
    summaryOpt match {
      case Some(summary) => logger.info(s"Stored object: ${summary.contentMd5}.")
      case None          => logger.info("Lock not acquired.")
    }

}
