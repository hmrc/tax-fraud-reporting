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

package integration

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.util.ByteString
import cats.data.EitherT
import cats.implicits._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.Assertion
import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.{Configuration, Logger}
import uk.gov.hmrc.mongo.lock.MongoLockRepository
import uk.gov.hmrc.objectstore.client.Path.{Directory, File}
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient
import uk.gov.hmrc.objectstore.client.{Md5Hash, ObjectSummaryWithMd5}
import uk.gov.hmrc.taxfraudreporting.models.Error
import uk.gov.hmrc.taxfraudreporting.services.{FraudReportStreamer, ObjectStorageWorker, SDESService}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class ObjectStorageWorkerSpec extends IntegrationSpecCommonBase with MockitoSugar {
  private val logger = Logger(getClass)

  private implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  private implicit val actorSystem: ActorSystem           = app.injector.instanceOf[ActorSystem]

  private val configuration  = app.injector.instanceOf[Configuration]
  private val lockRepository = app.injector.instanceOf[MongoLockRepository]

  private val testString = "qwertyuiop"

  override def afterEach(): Unit = {
    super.afterEach()
    logger.info("Releasing lock.")
    await(lockRepository.releaseLock("lockID", "owner"))
    logger.info(s"Locks after: ${lockRepository.collection.find().toFuture.futureValue}")
  }

  "ObjectStorageWorker's materialised value" should {

    def objectSummaryWhen(shouldBeLocked: Boolean)(test: Option[ObjectSummaryWithMd5] => Assertion): Unit = {
      val mockFraudReportStreamer = mock[FraudReportStreamer]

      def mockFraudReportSource = Source single ByteString(testString)
      when {
        mockFraudReportStreamer.stream(any(), any())
      } thenReturn mockFraudReportSource

      val mockObjectSummary     = ObjectSummaryWithMd5(File(Directory("/tmp"), "file1.dat"), 0, Md5Hash(testString), null)
      val mockObjectStoreClient = mock[PlayObjectStoreClient]
      when {
        mockObjectStoreClient.putObject(any(), any(), any(), any(), any(), any())(any(), any())
      } thenReturn
        Future.successful(mockObjectSummary)

      val mockSDESService = mock[SDESService]

      when {
        mockSDESService.fileNotify(any())(any())
      } thenReturn
        EitherT.right[Error](Future.successful(()))

      if (shouldBeLocked)
        await(lockRepository.takeLock("lockID", "owner", 1.hours))

      val isLocked = lockRepository.isLocked("lockID", "owner").futureValue
      logger.info(s"Is locked? $isLocked")
      logger.info(s"Locks before: ${lockRepository.collection.find().toFuture.futureValue}")
      logger.info(s"Should be locked? $shouldBeLocked")
      isLocked mustBe shouldBeLocked

      val objectStorageWorker =
        new ObjectStorageWorker(
          configuration,
          mockFraudReportStreamer,
          lockRepository,
          mockObjectStoreClient,
          mockSDESService
        ) {
          override val delay = 0
        }

      val objectSummaryOption = objectStorageWorker.queue.pull().futureValue.value
      logger.info(objectSummaryOption.toString)

      test(objectSummaryOption)
    }

    "be Some object summary on job completion" ignore objectSummaryWhen(shouldBeLocked = false) {
      _ mustBe defined
    }

    "be None on attempt of a job with lock in place" in objectSummaryWhen(shouldBeLocked = true) {
      _ mustBe empty
    }

  }
}
