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

package uk.gov.hmrc.taxfraudreporting.data_formats

import play.api.libs.json.{Json, Reads}

import java.time.LocalDateTime

case class FraudReport(
  _id: Long,
  sentToSdes: Boolean,
  correlationId: String,
  evasionData: EvasionData,
  lastUpdated: LocalDateTime
)

object FraudReport {
  implicit val reads: Reads[FraudReport] = Json.reads
}
