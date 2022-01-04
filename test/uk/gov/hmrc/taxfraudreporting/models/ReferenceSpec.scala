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
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{EitherValues, OptionValues}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks._
import play.api.libs.json.{JsString, Json}
import play.api.mvc.PathBindable

class ReferenceSpec extends AnyFreeSpec with Matchers with EitherValues with OptionValues {

  "a fraud reference" - {

    "must be bound from a url path" in {

      val fraudReference = FraudReference(1234567890)

      val result = implicitly[PathBindable[FraudReference]]
        .bind("fraudReference", "XHPR1234567890")

      result.right.value mustEqual fraudReference
    }

    "must deserialise" in {

      val fraudReference = FraudReference(123)

      JsString(fraudReference.toString).as[FraudReference] mustEqual fraudReference
    }

    "must serialise" in {

      val fraudReference = FraudReference(123)

      Json.toJson(fraudReference) mustEqual JsString(fraudReference.toString)
    }

    "must generate a modulo 23 check character" in {

      FraudReference(1).checkCharacter mustEqual 'Y'
      FraudReference(2).checkCharacter mustEqual 'Q'
      FraudReference(3).checkCharacter mustEqual 'S'
      FraudReference(4).checkCharacter mustEqual 'Z'
      FraudReference(5).checkCharacter mustEqual 'W'
      FraudReference(6).checkCharacter mustEqual 'B'
      FraudReference(7).checkCharacter mustEqual 'D'
      FraudReference(8).checkCharacter mustEqual 'F'
      FraudReference(9).checkCharacter mustEqual 'H'
      FraudReference(10).checkCharacter mustEqual 'P'
      FraudReference(30).checkCharacter mustEqual 'V'
      FraudReference(50).checkCharacter mustEqual 'E'
      FraudReference(70).checkCharacter mustEqual 'K'
      FraudReference(80).checkCharacter mustEqual 'N'
      FraudReference(14).checkCharacter mustEqual 'A'
      FraudReference(15).checkCharacter mustEqual 'C'
      FraudReference(17).checkCharacter mustEqual 'G'
      FraudReference(18).checkCharacter mustEqual 'X'
      FraudReference(27).checkCharacter mustEqual 'J'
      FraudReference(28).checkCharacter mustEqual 'L'
      FraudReference(37).checkCharacter mustEqual 'M'
      FraudReference(47).checkCharacter mustEqual 'P'
      FraudReference(48).checkCharacter mustEqual 'R'
      FraudReference(49).checkCharacter mustEqual 'T'
      FraudReference(58).checkCharacter mustEqual 'Z'
    }

    "must fail to build from an input that is not 14 characters" in {
      FraudReference("1234567890123") must not be defined
      FraudReference("123456789012345") must not be defined
    }

    "must fail to build from an input that doesn't start with X" in {
      FraudReference("1234567890123") must not be defined
    }

    "must fail to build from an input that does not have P as the third character" in {
      FraudReference("XAXR1234567890") must not be defined
    }

    "must fail to build from an input that does not have R as the fourth character" in {
      FraudReference("XAPX1234567890") must not be defined
    }

    "must fail to build from an input that does not have ten digits as characters 5 to 14" in {
      FraudReference("XAPRX000000001") must not be defined
    }

    "must fail to build from an input that does not have the correct check character as the second character" in {
      FraudReference("XAPR0000000001") must not be defined
    }

    "must build from a valid input" in {

      FraudReference("XYPR0000000001").value mustEqual FraudReference(1)
    }

    "must convert to a string in the correct format" in {
      FraudReference(1).toString mustEqual "XYPR0000000001"
    }

    "must treat .apply and .toString as dual" in {

      val gen: Gen[FraudReference] =
        Gen.choose(0, Int.MaxValue).map(FraudReference(_))

      forAll(gen) {
        fraudReference: FraudReference =>
          FraudReference(fraudReference.toString).value mustBe fraudReference
      }
    }

    "must fail to build from inputs with invalid check characters" in {

      val gen: Gen[String] = for {
        fraudReference        <- Gen.choose(0, Int.MaxValue).map(FraudReference(_).toString)
        invalidCheckCharacter <- Gen.alphaUpperChar suchThat (_ != fraudReference(1))
      } yield fraudReference(0) + invalidCheckCharacter + fraudReference.takeRight(12)

      forAll(gen) {
        FraudReference(_) mustBe empty
      }
    }
  }
}
