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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.taxfraudreporting.repositories.FraudReportRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class FraudReportController @Inject() (cc: ControllerComponents, repository: FraudReportRepository)(implicit
  executionContext: ExecutionContext
) extends BackendController(cc) {

  val CorrelationIdKey = "X-Correlation-ID"

  def postFraudReport: Action[JsValue] = Action.async(parse.json) { request =>
    request.headers.get(CorrelationIdKey) match {
      case Some(correlation_id) =>
        repository.insert(request.body, correlation_id, sentToSdes = false) map { result =>
          result.fold(
            errors => BadRequest(Json.obj("errors" -> Json.toJson(errors))),
            _ => Accepted(Json parse """{"success": "Fraud report submitted"}""")
          )
        }
      case None =>
        Future.successful {
          BadRequest(Json.obj("errors" -> Json.arr("Missing X-Correlation-ID header")))
        }
    }
  }

}
