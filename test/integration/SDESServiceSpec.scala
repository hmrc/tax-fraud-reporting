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

import org.apache.http.HttpStatus
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.libs.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.taxfraudreporting.models.sdes.{FileAudit, FileChecksum, FileMetaData, SDESFileNotifyRequest}
import uk.gov.hmrc.taxfraudreporting.services.SDESService

class SDESServiceSpec extends AnyWordSpec with Matchers with WiremockSupport with GuiceOneServerPerSuite {

  override lazy val app: Application =
    GuiceApplicationBuilder()
      .configure("microservice.services.sdes.host" -> host, "microservice.services.sdes.port" -> wiremockPort)
      .build()

  val sdesService = app.injector.instanceOf[SDESService]

  val fileNotifyRequest = SDESFileNotifyRequest(
    "fraud-reporting",
    FileMetaData(
      "tax-fraud-reporting",
      "file1.dat",
      s"http://localhost:8464/object-store/object/tax-fraud-reporting/file1.dat",
      FileChecksum(value = "hashValue"),
      2000,
      List()
    ),
    FileAudit("uuid")
  )

  val sdesUrl = "/sdes-stub/notification/fileready"
  val json    = Json.toJson(fileNotifyRequest).toPrettyString

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "SDESServiceSpec" should {

    "successfully notify SDES service with the details of file on ObjectStore" in {
      stubPost(sdesUrl, json, HttpStatus.SC_NO_CONTENT)
      val notificationResponse = sdesService.fileNotify(fileNotifyRequest)
      await(notificationResponse) shouldBe ()
    }

    "fail when non 204 response is received while notifying SDES service" in {
      val httpErrorStatus   = HttpStatus.SC_INTERNAL_SERVER_ERROR
      val httpErrorResponse = "Internal Server Error"
      stubPostWithResponse(sdesUrl, json, httpErrorStatus, httpErrorResponse)
      val notificationResponse = sdesService.fileNotify(fileNotifyRequest)
      whenReady(notificationResponse.failed) { e =>
        e shouldBe an[Exception]
        e.getMessage shouldBe (s"Exception in notifying SDES. Received http status: $httpErrorStatus body: $httpErrorResponse")
      }
    }
  }
}
