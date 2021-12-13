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

package uk.gov.hmrc.taxfraudreporting.integration

import akka.stream.Materializer
import org.scalatest.Inside.inside
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.taxfraudreporting.models.FraudReport
import uk.gov.hmrc.taxfraudreporting.repositories.{FraudReferenceRepository, FraudReportRepositoryImpl}
import uk.gov.hmrc.taxfraudreporting.services.JsonValidationService

import scala.concurrent.ExecutionContext
import scala.language.{implicitConversions, postfixOps}

class FraudReportRepositorySpec extends IntegrationSpecCommonBase with DefaultPlayMongoRepositorySupport[FraudReport] {
  private implicit val ec: ExecutionContext = injector.instanceOf[ExecutionContext]
  private implicit val mat: Materializer    = injector.instanceOf[Materializer]
  private val validationService             = injector.instanceOf[JsonValidationService]
  private val fraudReferenceService         = injector.instanceOf[FraudReferenceRepository]

  override def repository = new FraudReportRepositoryImpl(mongoComponent, validationService, fraudReferenceService)

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

    val correlationId = "fe28db96-d9db-4220-9e12-f2d267267c29"

    List("business", "person") foreach { dataName =>
      val fileName    = s"example-$dataName.json"
      val stream      = getClass.getClassLoader getResourceAsStream fileName
      val exampleData = (Json parse stream).as[JsObject]

      s"insert and remove example $dataName fraud report" in {

        await(repository.collection.drop().toFuture())

        val app       = builder.build()
        val inputData = exampleData

        running(app) {

          val document = repository.insert(inputData, correlationId, sentToSdes = false).futureValue.right.get

          inside(document) {
            case FraudReport(_id, _, _, cid, body, _) =>
              _id mustEqual document._id
              cid mustEqual correlationId
              body mustEqual inputData

              repository.remove(document._id).futureValue
              repository.get(document._id).futureValue mustBe empty
          }
        }
      }
    }

    "return errors given an invalid fraud report" in {
      val invalidData = Json.arr()

      running(app) {

        val dbInsertResult = repository.insert(invalidData, correlationId, sentToSdes = false).futureValue

        dbInsertResult.toOption mustBe empty
      }
    }
  }
}