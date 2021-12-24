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

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.taxfraudreporting.models.FraudReferenceJson
import uk.gov.hmrc.taxfraudreporting.repositories.SequentialFraudReferenceRepository

import scala.concurrent.ExecutionContext

class ReferenceRepositorySpec
    extends IntegrationSpecCommonBase with DefaultPlayMongoRepositorySupport[FraudReferenceJson] {

  private val executionContext = injector.instanceOf[ExecutionContext]
  override def repository      = new SequentialFraudReferenceRepository(mongoComponent)(executionContext)

  private lazy val builder = new GuiceApplicationBuilder()

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

  "a charge reference service" should {

    "return sequential ids" in {

      await(repository.collection.drop().toFuture())

      val app = builder.build()

      running(app) {

        val first  = repository.nextChargeReference().futureValue
        val second = repository.nextChargeReference().futureValue

        (second.value - first.value) mustBe 1
      }
    }

    "not fail if the collection already has a document on startup" in {
      await(repository.collection.drop().toFuture())

      repository.collection.insertOne(FraudReferenceJson("counter", 10))

      val app = builder.build()

      running(app) {

        repository.nextChargeReference().futureValue
      }
    }
  }
}
