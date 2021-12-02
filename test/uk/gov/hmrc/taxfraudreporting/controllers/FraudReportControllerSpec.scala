/*
 * Copyright 2021 HM Revenue & Customs
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

import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.taxfraudreporting.services.JsonValidationService

class FraudReportControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar {
  private val mockJson = Json.obj()

  private def serviceWhoseValidatorReturns(mockErrors: List[String]) = {
    val schemaName        = "fraudReport_schema"
    val validationService = mock[JsonValidationService]
    val validator         = mock[validationService.Validator]

    when(validator validate mockJson) thenReturn mockErrors
    when(validationService getValidator schemaName) thenReturn validator

    validationService
  }

  private val successfulValidationService = serviceWhoseValidatorReturns(mockErrors = Nil)

  "GET /hello-world" should {

    val fakeRequest = FakeRequest("GET", "/hello-world")
    val controller  = new FraudReportController(stubControllerComponents(), successfulValidationService)

    "return 200" in {
      val result = controller.hello(fakeRequest)
      status(result) shouldBe OK
    }
  }

  "POST /create-report" should {
    val postReportURL = uk.gov.hmrc.taxfraudreporting.controllers.routes.FraudReportController.postReport().url

    val mockJsonRequest =
      FakeRequest("Post", postReportURL) withBody mockJson withHeaders "Content-Type" -> "application/json"

    "return 200 given a valid fraud report" in {
      val controller = new FraudReportController(stubControllerComponents(), successfulValidationService)
      val result     = controller.postReport(mockJsonRequest)
      status(result) shouldBe OK
    }

    "return 400 given an invalid fraud report" in {
      val failingValidationService = serviceWhoseValidatorReturns(List("Validation error"))

      val controller = new FraudReportController(stubControllerComponents(), failingValidationService)
      val result     = controller.postReport(mockJsonRequest)
      status(result) shouldBe BAD_REQUEST
    }
  }
}
