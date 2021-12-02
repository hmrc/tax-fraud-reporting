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

package services

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import uk.gov.hmrc.taxfraudreporting.services.JsonValidationService

class ValidationServiceSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  private lazy val validationService = app.injector.instanceOf[JsonValidationService]

  "a validator" must {
    val validator = validationService getValidator "test-schema"

    "return an empty list of errors when a document is valid" in {

      val json = Json parse """{"simpleDeclarationRequest": {"foo": "bar"}}"""

      validator validate json mustBe empty
    }

    "return a list of validation errors when a document is invalid" in {

      val json = Json.obj()

      validator.validate(json) must not be empty
    }
  }

  "Fraud report schema" must {
    val validator = validationService getValidator "fraud-report.schema"

    "validate example business JSON" in {
      val jsonStream = getClass getResourceAsStream "/example-business.json"

      validator.validate(Json parse jsonStream) mustBe empty
    }

    "validate example person JSON" in {
      val jsonStream = getClass getResourceAsStream "/example-person.json"

      validator.validate(Json parse jsonStream) mustBe empty
    }
  }
}
