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
import scala.concurrent.ExecutionContext

@Singleton()
class FraudReportController @Inject() (cc: ControllerComponents, repository: FraudReportRepository)(implicit
  executionContext: ExecutionContext
) extends BackendController(cc) {

  def createFraudReport: Action[JsValue] = Action.async(parse.json) { request =>
    repository.insert(request.body) map { result =>
      result.fold(
        errors => BadRequest(Json.obj("errors" -> Json.arr(errors))),
        _ => Created(Json.obj("success" -> "Fraud report submitted"))
      )
    }
  }

}
