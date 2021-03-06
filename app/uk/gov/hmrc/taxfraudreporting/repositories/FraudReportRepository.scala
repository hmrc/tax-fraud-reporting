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

package uk.gov.hmrc.taxfraudreporting.repositories

import com.google.inject.ImplementedBy
import org.mongodb.scala.result.UpdateResult
import org.mongodb.scala.{FindObservable, SingleObservable}
import uk.gov.hmrc.taxfraudreporting.models.{FraudReport, FraudReportBody}

import java.util.UUID
import scala.concurrent.Future

@ImplementedBy(classOf[FraudReportRepositoryImpl])
trait FraudReportRepository {

  def insert(data: FraudReportBody): Future[FraudReport]

  def get(id: UUID): Future[Option[FraudReport]]

  def remove(id: UUID): Future[Option[FraudReport]]

  def listUnprocessed: FindObservable[FraudReport]

  def countUnprocessed: SingleObservable[Long]

  def updateUnprocessed(correlationId: UUID): Future[UpdateResult]

  def updateAsProcessed(correlationId: UUID): Future[UpdateResult]
}
