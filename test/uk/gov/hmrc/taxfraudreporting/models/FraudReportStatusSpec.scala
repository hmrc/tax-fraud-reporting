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

package uk.gov.hmrc.taxfraudreporting.models

import org.scalacheck.Gen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json._

class FraudReportStatusSpec extends AnyFlatSpec with Matchers {

  private def assertIsInvalid(jsValue: JsValue) =
    assert(jsValue.validate[FraudReportStatus].isError)

  "FraudReportStatus enum" should "give a JsError when reading a non-JsString" in {
    val invalidJsValues = List(Json.obj(), Json.arr())

    invalidJsValues foreach assertIsInvalid
  }

  it should "give a JsError when reading an invalid string" in {
    val validStrings     = FraudReportStatus.values.map(_.toString)
    val invalidJsStrings = Gen.alphaStr suchThat { str => !(validStrings contains str) } map JsString

    forAll(invalidJsStrings)(assertIsInvalid)
  }

  it should "treat JS Reads and Writes functions as inverses" in {
    FraudReportStatus.values foreach { listedValue =>
      val JsSuccess(recoveredValue, _) = Json.toJson(listedValue).validate[FraudReportStatus]

      recoveredValue shouldBe listedValue
    }
  }
}
