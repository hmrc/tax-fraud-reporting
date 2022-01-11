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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.taxfraudreporting.mocks.MockFraudReportRepository

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class FraudReportControllerSpec extends AnyWordSpec with Matchers {

  "POST /create-report" should {
    val postFraudReportURL =
      uk.gov.hmrc.taxfraudreporting.controllers.routes.FraudReportController.createFraudReport().url

    def mockJsonRequest(data: JsValue, withCid: Boolean = true) = {
      val req = FakeRequest("Post", postFraudReportURL) withBody data withHeaders "Content-Type" -> "application/json"

      if (withCid)
        req withHeaders "X-Correlation-ID" -> ""
      else
        req
    }

    val succeedingRepo                        = new MockFraudReportRepository(true)
    implicit val ec: ExecutionContextExecutor = ExecutionContext.global

    val requestWithObj = mockJsonRequest(Json.obj())

    "respond 202 Accepted given a valid fraud report" in {
      val controller = new FraudReportController(stubControllerComponents(), succeedingRepo)

      val result = controller.createFraudReport(requestWithObj)
      status(result) shouldBe CREATED
    }

    "respond 400 Bad Request when given an invalid fraud report" in {
      val failingRepo = new MockFraudReportRepository(false)
      val controller  = new FraudReportController(stubControllerComponents(), failingRepo)

      val result = controller.createFraudReport(requestWithObj)
      status(result) shouldBe BAD_REQUEST
    }

    "respond 400 Bad Request when missing X-Correlation-ID header" in {
      val requestSansCid = mockJsonRequest(Json.arr(), withCid = false)
      val controller     = new FraudReportController(stubControllerComponents(), succeedingRepo)

      val result = controller.createFraudReport(requestSansCid)
      status(result) shouldBe BAD_REQUEST
    }
  }
}
