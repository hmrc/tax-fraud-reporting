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

package shared

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import uk.gov.hmrc.taxfraudreporting.models.xml._
import uk.gov.hmrc.taxfraudreporting.models.{FraudReport, FraudReportBody}

import java.time.LocalDateTime
import scala.math.BigDecimal.RoundingMode

trait GenDriven {

  private def strWithMaxLength(maxLength: Int, charGen: Gen[Char] = Gen.alphaNumChar) = for {
    n              <- Gen.chooseNum(1, maxLength)
    alphaNumString <- charsN(n, charGen)
  } yield alphaNumString

  private val asciiPrintableStrWithMax255Chars = strWithMaxLength(255, Gen.asciiPrintableChar)
  private val stringOptions                    = Gen option strWithMaxLength(255)

  private val phoneNumOptions = Gen option {
    Gen.listOfN(11, Gen.numChar) map { chars =>
      String.copyValueOf(chars.toArray)
    }
  }

  private val stringsOfMin2Chars = for {
    char0 <- Gen.alphaChar
    char1 <- Gen.alphaChar
    chars <- Gen.alphaStr
  } yield char0 +: char1 +: chars

  private val emails = for {
    user   <- stringsOfMin2Chars
    domain <- stringsOfMin2Chars
    ext    <- stringsOfMin2Chars
  } yield s"$user@$domain.$ext"

  private val reporters = for {
    firstName     <- stringOptions
    lastName      <- stringOptions
    phone         <- phoneNumOptions
    email         <- Gen option emails
    memorableWord <- Gen option strWithMaxLength(255)
  } yield Reporter(firstName, lastName, phone, email, memorableWord)

  private def charsN(n: Int, gen: Gen[Char]) = Gen.containerOfN[Array, Char](n, gen) map String.valueOf

  private val subjectPayeRefs = for {
    part1 <- charsN(3, Gen.numChar)
    part2 <- charsN(2, Gen.alphaUpperChar)
    part3 <- charsN(3, Gen.numChar)
  } yield part1 + "/" + part2 + part3

  private val vatNumbers = for {
    chars  <- charsN(2, Gen.alphaUpperChar)
    digits <- charsN(10, Gen.numChar)
  } yield chars + digits

  private val niNumbers = for {
    chars  <- charsN(2, Gen.alphaUpperChar)
    digits <- charsN(6, Gen.numChar)
    char3  <- Gen.alphaUpperChar
  } yield chars + digits :+ char3

  private val dateStrings = for {
    day   <- Gen oneOf (1 to 31)
    month <- Gen oneOf (1 to 12)
    year  <- Gen oneOf (1900 to 2022)
  } yield f"$day%02d/$month%02d/$year%4d"

  private val contactOptions = Gen option {
    for {
      landline <- phoneNumOptions
      mobile   <- phoneNumOptions
      email    <- Gen option emails
    } yield Contact(landline, mobile, email)
  }

  private val addressOptions = Gen option {
    for {
      line1      <- stringOptions
      line2      <- stringOptions
      line3      <- stringOptions
      townOrCity <- stringOptions
      postCode   <- stringOptions
      country    <- stringOptions
    } yield Address(line1, line2, line3, townOrCity, postCode, country)
  }

  private val names = for {
    firstName  <- stringOptions
    middleName <- stringOptions
    lastName   <- stringOptions
    nickname   <- stringOptions
  } yield Name(firstName, middleName, lastName, nickname)

  private val people = for {
    name           <- Gen option names
    address        <- addressOptions
    contact        <- contactOptions
    dob            <- Gen option dateStrings
    age            <- Gen option Gen.chooseNum(18, 122)
    nino           <- Gen option niNumbers
    connectionType <- asciiPrintableStrWithMax255Chars
  } yield Person(name, address, contact, dob, age, nino, connectionType = connectionType)

  private val businesses = for {
    name           <- stringOptions
    businessType   <- stringOptions
    address        <- addressOptions
    contact        <- contactOptions
    vatNo          <- Gen option vatNumbers
    ctUTR          <- Gen option charsN(10, Gen.numChar)
    payeNo         <- Gen option subjectPayeRefs
    connectionType <- asciiPrintableStrWithMax255Chars
  } yield Business(name, businessType, address, contact, vatNo, ctUTR, payeNo, connectionType)

  private val nominals = for {
    personOpt   <- Gen option people
    businessOpt <- Gen option businesses
  } yield Nominal(personOpt, businessOpt)

  private def nonEmptyListsOf[T](gen: Gen[T], max: Int) =
    Gen.choose(1, max) flatMap {
      Gen.listOfN(_, gen)
    }

  private val listsOfNominals = nonEmptyListsOf(nominals, 5)

  val maxFraudValue = Math.pow(10, 12) - 2

  private val decimals =
    Gen.chooseNum(0.01, maxFraudValue)
      .map(BigDecimal(_).setScale(2, RoundingMode.DOWN))

  private val fraudReportBodies = for {
    activityType      <- strWithMaxLength(255)
    informationSource <- strWithMaxLength(255)
    listOfNominals    <- listsOfNominals
    valueFraud        <- Gen option decimals
    durationFraud     <- stringOptions
    howManyKnow       <- stringOptions
    additionalDetails <- stringOptions
    reporter          <- Gen option reporters
    hasEvidence       <- arbitrary[Boolean]
    evidenceDetails   <- Gen.option(strWithMaxLength(500)).map(_.filter(_ => hasEvidence))
  } yield FraudReportBody(
    activityType,
    listOfNominals,
    informationSource,
    evidenceDetails,
    valueFraud,
    durationFraud,
    howManyKnow,
    additionalDetails,
    reporter,
    hasEvidence
  )

  private val fraudReports = for {
    body        <- fraudReportBodies
    isProcessed <- arbitrary[Boolean]
  } yield FraudReport(body, LocalDateTime.now(), isProcessed)

  val listsOfFraudReports: Gen[List[FraudReport]] = nonEmptyListsOf(fraudReports, 99)

}
