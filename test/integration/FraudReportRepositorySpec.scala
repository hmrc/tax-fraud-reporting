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

package integration

import org.mongodb.scala.model.{Filters, Updates}
import org.scalatest.Inside.inside
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.taxfraudreporting.models.{FraudReport, FraudReportBody}
import uk.gov.hmrc.taxfraudreporting.repositories.FraudReportRepositoryImpl

import java.util.UUID
import scala.concurrent.ExecutionContext

class FraudReportRepositorySpec
    extends IntegrationSpecCommonBase with DefaultPlayMongoRepositorySupport[FraudReport] with OptionValues {
  private implicit val ec: ExecutionContext = injector.instanceOf[ExecutionContext]

  override def repository = new FraudReportRepositoryImpl(mongoComponent)

  override def beforeAll(): Unit =
    super.beforeAll()

  override def beforeEach(): Unit =
    super.beforeEach()

  override def afterEach(): Unit = {
    super.afterEach()
    await(repository.collection.drop().toFuture())
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(repository.collection.drop().toFuture())
  }

  lazy val builder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()

  "a fraud report repository" should {
    List("business", "person") foreach { dataName =>
      val fileName    = s"example-$dataName.json"
      val stream      = getClass.getClassLoader getResourceAsStream fileName
      val exampleData = (Json parse stream).as[FraudReportBody]

      s"insert and remove example $dataName fraud report" in {

        await(repository.collection.drop().toFuture())

        val app       = builder.build()
        val inputData = exampleData

        running(app) {

          val document = repository.insert(inputData).futureValue

          inside(document) {
            case FraudReport(body, _, _, _, _id) =>
              _id mustEqual document._id
              body mustEqual inputData

              repository.remove(document._id).futureValue
              repository.get(document._id).futureValue mustBe empty
          }
        }
      }

      s"update unprocessed $dataName reports with correlation id" in {
        await(repository.collection.drop().toFuture())

        val app       = builder.build()
        val inputData = exampleData
        running(app) {

          val unprocessedDocument = repository.insert(inputData).futureValue.right.get

          val docToSetAsProcessed = repository.insert(inputData).futureValue.right.get
          val updatedResult = repository.collection.updateOne(
            Filters.equal("_id", docToSetAsProcessed._id.toString),
            Updates.set("isProcessed", true)
          ).toFuture().futureValue
          updatedResult.getModifiedCount mustBe 1

          val correlationId  = UUID.randomUUID()
          val eventualResult = repository.updateUnprocessed(correlationId).futureValue
          eventualResult.getModifiedCount mustBe 1

          val updated = repository.listUnprocessed.toFuture().futureValue
          updated.size mustBe 1
          updated.head._id mustBe unprocessedDocument._id
          updated.head.correlationId.value should be(correlationId)
        }
      }
    }
  }
}
