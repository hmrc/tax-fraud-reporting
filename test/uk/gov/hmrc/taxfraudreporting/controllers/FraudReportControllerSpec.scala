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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.taxfraudreporting.models.{FraudReport, FraudReportBody}
import uk.gov.hmrc.taxfraudreporting.repositories.FraudReportRepository

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

class FraudReportControllerSpec extends AnyWordSpec with Matchers with MockitoSugar {
  "POST /create-report" should {
    val postFraudReportURL =
      uk.gov.hmrc.taxfraudreporting.controllers.routes.FraudReportController.createFraudReport().url

    implicit val ec: ExecutionContextExecutor = ExecutionContext.global
    implicit val as: ActorSystem              = ActorSystem()

    val repo = mock[FraudReportRepository]
    val body = FraudReportBody("", Nil)
    when(repo.insert(any())) thenReturn Future.successful(FraudReport(body))

    val controller = new FraudReportController(stubControllerComponents(), repo)

    "respond 201 Created given a valid fraud report" in {
      val request = FakeRequest("Post", postFraudReportURL) withBody
        body withHeaders "Content-Type" -> "application/json"
      val result = controller.createFraudReport(request)

      status(result) shouldBe CREATED
    }

    "respond 400 Bad Request when given an invalid fraud report" in {
      val request = FakeRequest("Post", postFraudReportURL) withBody
        Json.obj() withHeaders "Content-Type" -> "application/json"
      val result = controller.createFraudReport(request)
      status(result) shouldBe BAD_REQUEST
    }
  }
}
