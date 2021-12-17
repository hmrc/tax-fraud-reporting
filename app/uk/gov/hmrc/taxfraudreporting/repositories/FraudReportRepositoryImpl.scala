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

package uk.gov.hmrc.taxfraudreporting.repositories

import com.google.inject.{Inject, Singleton}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import play.api.libs.json.JsValue
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.taxfraudreporting.models.{FraudReference, FraudReport}
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
      indexes = Seq(
        IndexModel(ascending("sentToSdes"), IndexOptions().name("fraud-reports-sent-to-sdes")),
        IndexModel(ascending("isProcessed"), IndexOptions().name("processed-fraud-reports"))
      )
    ) with FraudReportRepository {

  private val validator = validationService getValidator "fraud-report.schema"

  def insert(
    fraudReportBody: JsValue,
    correlationId: String,
    sentToSdes: Boolean
  ): Future[Either[List[String], FraudReport]] =
    fraudReferenceService.nextChargeReference() flatMap {
      id =>
        val validationErrors = validator validate fraudReportBody

        if (validationErrors.isEmpty) {

          val fraudReport =
            FraudReport(id, sentToSdes, isProcessed = false, correlationId, fraudReportBody, LocalDateTime.now())

          collection.insertOne(fraudReport).toFuture() map { _ => Right(fraudReport) }
        } else
          Future.successful(Left(validationErrors))
    }

  def get(id: FraudReference): Future[Option[FraudReport]] =
    collection.find(equal("_id", id.toString)).first().toFutureOption()

  def remove(id: FraudReference): Future[Option[FraudReport]] =
    collection.findOneAndDelete(equal("_id", Codecs.toBson(id))).headOption()

}
