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

import com.google.inject.{Inject, Singleton}
import org.mongodb.scala.model.Filters.{and, equal}
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import play.api.libs.json.JsValue
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.taxfraudreporting.models.{FraudReference, FraudReport, FraudReportStatus}
import uk.gov.hmrc.taxfraudreporting.services.JsonValidationService

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FraudReportRepositoryImpl @Inject() (
  mongoComponent: MongoComponent,
  validationService: JsonValidationService,
  fraudReferenceService: FraudReferenceRepository
)(implicit val ec: ExecutionContext)
    extends PlayMongoRepository[FraudReport](
      collectionName = "fraud-reports",
      mongoComponent = mongoComponent,
      domainFormat = FraudReport.format,
      indexes = Seq(IndexModel(ascending("status"), IndexOptions().name("fraud-report-status")))
    ) with FraudReportRepository {

  private val validator = validationService getValidator "fraud-report.schema"

  def insert(reportBody: JsValue, reportId: String): Future[Either[List[String], FraudReport]] =
    fraudReferenceService.nextChargeReference() flatMap {
      ref =>
        val validationErrors = validator validate reportBody

        if (validationErrors.isEmpty) {
          val fraudReport = FraudReport(ref, reportId, reportBody, LocalDateTime.now())

          collection.insertOne(fraudReport).toFuture() map { _ => Right(fraudReport) }
        } else
          Future.successful(Left(validationErrors))
    }

  def get(id: FraudReference): Future[Option[FraudReport]] =
    collection.find(equal("_id", id.toString)).first().toFutureOption()

  def update(id: FraudReference, status: FraudReportStatus): Future[Option[FraudReport]] =
    collection.findOneAndUpdate(equal("_id", id.toString), set("status", status.toString)).toFutureOption()

  def remove(id: FraudReference): Future[Option[FraudReport]] =
    collection.findOneAndDelete(
      and(equal("_id", id.toString), equal("status", FraudReportStatus.Processed.toString))
    ).headOption

}
