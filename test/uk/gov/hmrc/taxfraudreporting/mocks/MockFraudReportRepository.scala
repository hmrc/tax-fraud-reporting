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

package uk.gov.hmrc.taxfraudreporting.mocks

import play.api.libs.json.JsValue
import uk.gov.hmrc.taxfraudreporting.models.{FraudReference, FraudReport}
import uk.gov.hmrc.taxfraudreporting.repositories.FraudReportRepository

import java.time.LocalDateTime
import scala.concurrent.Future

class MockFraudReportRepository(succeeding: Boolean) extends FraudReportRepository {

  def insert(data: JsValue, mockString: String, sentToSdes: Boolean): Future[Either[List[String], FraudReport]] =
    Future.successful {
      if (succeeding)
        Right(FraudReport(FraudReference(0), sentToSdes = false, isProcessed = false, "", data, LocalDateTime.now()))
      else
        Left(List("Invalid JSON"))
    }

  def get(id: FraudReference): Future[Option[FraudReport]] = null

  def remove(id: FraudReference): Future[Option[FraudReport]] = null

}
