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
import play.api.libs.json.{JsNumber, JsString, JsSuccess, JsValue, Json}
import uk.gov.hmrc.taxfraudreporting.models.FraudReportStatusJsError.{InvalidJsStringValue, InvalidJsValueSubtype}

class FraudReportStatusSpec extends AnyFlatSpec with Matchers {
  private val writtenStrings  = FraudReportStatus.values map Json.toJson[FraudReportStatus]
  private val expectedStrings = List("Received", "Sent", "Processed", "Failed")

  "FraudStatusSpec enum" should "result in JsError when read from JS non-strings" in {
    val jsValues: List[JsValue] = List(Json.obj(), Json.arr(), JsNumber(1234))

    jsValues foreach { jsValue =>
      val jsResult = jsValue.validate[FraudReportStatus]
      jsResult shouldBe InvalidJsValueSubtype
    }
  }

  it should "also result in a JsError when read from an invalid string" in {
    val invalidJsStrings = Gen.alphaStr suchThat { str => !(expectedStrings contains str) } map JsString

    forAll(invalidJsStrings) { jsString =>
      val jsResult = jsString.validate[FraudReportStatus]

      jsResult shouldBe InvalidJsStringValue
    }
  }

  it should "be writeable to the correct JSON strings" in {
    writtenStrings zip expectedStrings foreach {
      case (fraudReportStatus, expectedString) =>
        val JsString(writtenString) = Json toJson fraudReportStatus

        writtenString shouldBe expectedString
    }
  }

  it should "be recoverable by reading after writing" in {
    FraudReportStatus.values foreach { listedValue =>
      val JsSuccess(recoveredValue, _) = Json.toJson(listedValue).validate[FraudReportStatus]

      recoveredValue shouldBe listedValue
    }
  }
}
