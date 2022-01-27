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

package uk.gov.hmrc.taxfraudreporting.models

import play.api.libs.json.{Format, JsError, JsString, JsSuccess}

sealed abstract class FraudReportStatus

object FraudReportStatus {
  case object Received  extends FraudReportStatus
  case object Sent      extends FraudReportStatus
  case object Processed extends FraudReportStatus
  case object Failed    extends FraudReportStatus

  val values: List[FraudReportStatus] = List(Received, Sent, Processed, Failed)

  implicit val format: Format[FraudReportStatus] = Format(
    _.validate[String] flatMap { value =>
      values find {
        _.toString == value
      } match {
        case Some(status) => JsSuccess(status)
        case None         => JsError("Invalid JsString value")
      }
    },
    status => JsString(status.toString)
  )

}
