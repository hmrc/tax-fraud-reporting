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
import org.mongodb.scala.model.{FindOneAndUpdateOptions, ReturnDocument, Updates}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.taxfraudreporting.models.{FraudReference, FraudReferenceJson}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SequentialFraudReferenceRepository @Inject() (mongoComponent: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[FraudReferenceJson](
      collectionName = "fraud-reference",
      mongoComponent = mongoComponent,
      domainFormat = FraudReferenceJson.format,
      indexes = Seq()
    ) with FraudReferenceRepository {

  private val id: String = "counter"

  val started: Future[Unit] =
    Future.successful(collection.insertOne(FraudReferenceJson.apply(id, 0)))

  override def nextChargeReference(): Future[FraudReference] =
    collection.findOneAndUpdate(
      equal("_id", id),
      Updates.inc("reference", 1),
      FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
    ).toFuture() map { fraudRef => FraudReference(fraudRef.reference) }

}
