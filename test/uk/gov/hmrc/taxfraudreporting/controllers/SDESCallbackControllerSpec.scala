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

package uk.gov.hmrc.taxfraudreporting.controllers

import akka.actor.ActorSystem
import ch.qos.logback.classic.Level
import org.mockito.ArgumentMatchers.{any, eq => equalTo}
import org.mockito.Mockito.{clearInvocations, verify, verifyNoInteractions, when}
import org.mongodb.scala.result.UpdateResult
import org.scalatest.Inspectors._
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, LoneElement}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration}
import uk.gov.hmrc.gg.test.LogCapturing
import uk.gov.hmrc.objectstore.client.Path
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient
import uk.gov.hmrc.taxfraudreporting.models.sdes.NotificationStatus.{
  FileProcessed,
  FileProcessingFailure,
  FileReady,
  FileReceived
}
import uk.gov.hmrc.taxfraudreporting.models.sdes.{CallBackNotification, NotificationStatus}
import uk.gov.hmrc.taxfraudreporting.repositories.FraudReportRepository

import java.util.UUID
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

class SDESCallbackControllerSpec
    extends AnyWordSpec with Matchers with GuiceOneServerPerSuite with LogCapturing with LoneElement
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
  implicit val as: ActorSystem              = ActorSystem()
  val mockObjectStoreClient                 = mock[PlayObjectStoreClient]
  val mockFraudReportRepository             = mock[FraudReportRepository]
  when {
    mockObjectStoreClient.deleteObject(any(), any())(any())
  } thenReturn
    Future.successful(())

  when {
    mockFraudReportRepository.updateUnprocessed(any())
  } thenReturn
    Future.successful(mock[UpdateResult])

  override lazy val app: Application =
    GuiceApplicationBuilder()
      .overrides(
        bind[PlayObjectStoreClient].toInstance(mockObjectStoreClient),
        bind[FraudReportRepository].toInstance(mockFraudReportRepository)
      ).build()

  val controller = app.injector.instanceOf[SDESCallbackController]

  val uuid = UUID.randomUUID()

  val fileName = "fileName1.dat"

  val configuration = app.injector.instanceOf[Configuration]
  val path          = Path Directory configuration.get[String]("services.objectStorageWorker.path")
  val filePath      = path file fileName

  val sdesCallbackURL =
    uk.gov.hmrc.taxfraudreporting.controllers.routes.SDESCallbackController.callback().url

  override def beforeEach(): Unit =
    clearInvocations(mockObjectStoreClient, mockFraudReportRepository)

  def createCallBackNotification(status: NotificationStatus, fileName: String) =
    CallBackNotification(status, fileName, uuid.toString, None)

  def performActionWithJsonBody(requestBody: CallBackNotification): Future[Result] = {
    val request = FakeRequest("Post", sdesCallbackURL).withBody(requestBody).withHeaders(CONTENT_TYPE -> JSON)
    controller.callback(request)
  }

  "POST /create-report" should {
    "return 200, log the details" when {
      "call back status is FileReady/FileReceived/FileProcessingFailure" in {
        val statuses = List(FileReady, FileReceived, FileProcessingFailure)
        forAll(statuses) { notificationStatus =>
          withCaptureOfLoggingFrom[SDESCallbackController] { logs =>
            val notification = createCallBackNotification(notificationStatus, fileName)
            val result       = performActionWithJsonBody(notification)
            eventually {
              logs.filter(
                _.getLevel == Level.INFO
              ).loneElement.getMessage shouldBe s"Received SDES callback for file: $fileName, with correlationId : $uuid and status : $notificationStatus"
              status(result) shouldBe OK
            }
            verifyNoInteractions(mockObjectStoreClient, mockFraudReportRepository)
          }
        }
      }

      "delete file from object store and update unprocessed in mongo when call back status is FileProcessed" in {
        withCaptureOfLoggingFrom[SDESCallbackController] { logs =>
          val notification = createCallBackNotification(FileProcessed, fileName)
          val result       = performActionWithJsonBody(notification)
          eventually {
            logs.filter(
              _.getLevel == Level.INFO
            ).loneElement.getMessage shouldBe s"Received SDES callback for file: $fileName, with correlationId : $uuid and status : $FileProcessed"
            status(result) shouldBe OK
          }
          verify(mockObjectStoreClient).deleteObject(equalTo(filePath), any())(any())
          verify(mockFraudReportRepository).updateUnprocessed(equalTo(UUID.fromString(notification.correlationID)))
        }
      }
    }

    "return 400  Bad Request when given an invalid Callback notification " in {
      val request = FakeRequest("Post", sdesCallbackURL).withBody(Json.obj()).withHeaders(CONTENT_TYPE -> JSON)
      val result  = controller.callback(request)

      status(result) shouldBe BAD_REQUEST
      verifyNoInteractions(mockObjectStoreClient, mockFraudReportRepository)
    }

  }
}
