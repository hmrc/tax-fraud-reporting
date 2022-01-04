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

import play.api.libs.json.{__, JsError, JsString, JsSuccess, Reads, Writes}
import play.api.mvc.PathBindable

import scala.collection.Map

final case class FraudReference(value: Int) {

  override def toString: String = s"X${checkCharacter}PR$paddedValue"

  private def paddedValue = f"$value%010d"

  val checkCharacter: Char = {

    val weights = Seq(9, 10, 11, 12, 13, 8, 7, 6, 5, 4, 3, 2)

    val equivalentValues = Seq(48, 50) ++ paddedValue.map(_.asDigit)

    val remainder = weights.zip(equivalentValues).map {
      case (a, b) => a * b
    }.sum % 23

    FraudReference.checkCharacterMap(remainder)
  }

}

object FraudReference {

  private val referenceFormat = """^X([A-Z])PR(\d{10})$""".r

  def apply(input: String): Option[FraudReference] = input match {
    case referenceFormat(checkChar, digits) =>
      val fraudReference = FraudReference(digits.toInt)

      if (fraudReference.checkCharacter.toString == checkChar)
        Some(fraudReference)
      else
        None
    case _ =>
      None
  }

  implicit lazy val reads: Reads[FraudReference] =
    __.read[String] flatMap { referenceString =>
      Reads { _ =>
        apply(referenceString) match {
          case Some(ref) => JsSuccess(ref)
          case None      => JsError("Invalid charge reference")
        }
      }
    }

  implicit lazy val writes: Writes[FraudReference] =
    Writes { fraudReference =>
      JsString(fraudReference.toString)
    }

  implicit def pathBindable: PathBindable[FraudReference] = new PathBindable[FraudReference] {

    override def bind(key: String, value: String): Either[String, FraudReference] =
      FraudReference.apply(value).toRight("Invalid charge reference")

    override def unbind(key: String, value: FraudReference): String =
      value.toString

  }

  private val checkCharacterMap = Map(
    0  -> 'A',
    1  -> 'B',
    2  -> 'C',
    3  -> 'D',
    4  -> 'E',
    5  -> 'F',
    6  -> 'G',
    7  -> 'H',
    8  -> 'X',
    9  -> 'J',
    10 -> 'K',
    11 -> 'L',
    12 -> 'M',
    13 -> 'N',
    14 -> 'Y',
    15 -> 'P',
    16 -> 'Q',
    17 -> 'R',
    18 -> 'S',
    19 -> 'T',
    20 -> 'Z',
    21 -> 'V',
    22 -> 'W'
  )

}
