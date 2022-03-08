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
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions, Updates}
import org.mongodb.scala.result.UpdateResult
import org.mongodb.scala.{FindObservable, SingleObservable}
import play.api.Configuration
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.taxfraudreporting.models.{FraudReport, FraudReportBody}

import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FraudReportRepositoryImpl @Inject() (mongoComponent: MongoComponent, configuration: Configuration)(implicit val ec: ExecutionContext)
    extends PlayMongoRepository[FraudReport](
      collectionName = "fraudReports",
      mongoComponent = mongoComponent,
      domainFormat = FraudReport.format,
      indexes = Seq(
        IndexModel(ascending("correlationId")),
        IndexModel(ascending("isProcessed")),
        IndexModel(
          ascending("submitted"),
          IndexOptions()
            .name("submittedIdx")
            .expireAfter(configuration.get[Duration]("fraud-report-ttl").toSeconds, TimeUnit.SECONDS)
        )
      )
    ) with FraudReportRepository {

  def insert(reportBody: FraudReportBody): Future[FraudReport] = {
    val fraudReport = FraudReport(reportBody)

    collection.insertOne(fraudReport).toFuture map { _ => fraudReport }
  }

  def get(id: UUID): Future[Option[FraudReport]] =
    collection.find(equal("_id", id.toString)).first().toFutureOption()

  def remove(id: UUID): Future[Option[FraudReport]] =
    collection.findOneAndDelete(equal("_id", id.toString)).headOption

  private val unprocessed = equal("isProcessed", false)

  def listUnprocessed: FindObservable[FraudReport] =
    collection find unprocessed

  def countUnprocessed: SingleObservable[Long] =
    collection countDocuments unprocessed

  def updateUnprocessed(correlationId: UUID): Future[UpdateResult] =
    collection.updateMany(unprocessed, Updates.set("correlationId", correlationId.toString)).toFuture()

  def updateAsProcessed(correlationId: UUID): Future[UpdateResult] =
    collection.updateMany(equal("correlationId", correlationId.toString), Updates.set("isProcessed", true)).toFuture()

}
