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

package uk.gov.hmrc.taxfraudreporting.xml.mapping

import play.api.libs.json.{JsArray, JsDefined, JsLookup, JsLookupResult, JsUndefined, JsValue, Json}
import play.api.libs.json.JsValue.jsValueToJsLookup
import uk.gov.hmrc.taxfraudreporting.models.FraudReport
import uk.gov.hmrc.taxfraudreporting.xml.models.{
  Address,
  Business,
  Contact,
  FileBody,
  FileHeader,
  FraudReports,
  Name,
  Nominals,
  Person,
  Reporter
}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.language.implicitConversions

object FraudReportMapper {

  implicit def toOpt[A](a: A): Option[A] = Some(a)

  def getXmlFraudReports(data: Seq[FraudReport]): FraudReports = {

    val fileHeader = getFileHeader(data)

    val fileBodies: Seq[FileBody] = data map getFileBody

    FraudReports(fileHeader, fileBodies)
  }

  def getFileHeader(data: Seq[FraudReport]): FileHeader = {
    val timeNow                     = LocalDateTime.now();
    def now(format: String): String = timeNow.format(DateTimeFormatter.ofPattern(format))

    FileHeader(
      sending_System = "Digital RIS Fraud Reporting",
      receiving_System = "EVI BDApp",
      extract_Date_Time = now("dd/MM/yyyy HH:mm:ss"),
      filename =
        s"DIGITAL_EVIBDAPP_${now("yyyyMMddHHmm")}_FRAUD_REPORTS.xml", //Fine name can be moved out & get as input from scheduler job
      num_Reports = data.size.toString,
      file_Version = "0.1"
    )

  }

  def getFileBody(data: FraudReport): FileBody = {
    val jsonData: JsLookup = jsValueToJsLookup(data.body)

    FileBody(
      report_Number = data._id.toString,
      digital_ID = data._id.toString, //Need to confirm with business
      submitted = data.submitted.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
      activity_Type = jsonData.\("activity_Type").as[String],
      nominals = getNominal(jsonData),
      value_Fraud = jsonData.\("value_Fraud").as[String],
      value_Fraud_Band = jsonData.\("value_Fraud_Band").asOpt[String],
      duration_Fraud = jsonData.\("duration_Fraud").as[String],
      how_Many_Knew = jsonData.\("how_Many_Knew").as[String],
      additional_Details = jsonData.\("additional_Details").asOpt[String],
      reporter = getReporter(jsonData.\("reporter")),
      supporting_Evidence = jsonData.\("supporting_Evidence").asOpt[Boolean]
    )
  }

  def getNominal(data: JsLookup): Nominals = {

    val persons: JsArray = data.\("person") match {
      case JsDefined(value) => value.as[JsArray]
      case _: JsUndefined   => Json.arr()
    }

    val personsXml: Seq[Person]    = persons.value.map(value => getPersons(value))
    val business: Option[Business] = getBusiness(data.\("business"))

    Nominals(personsXml, business)
  }

  def getPersons(person: JsValue): Person =
    if (person.isDefined)
      Person(
        name = getName(person.\("name")),
        address = getAddress(person.\("address")),
        contact = getContact(person.\("contact")),
        dob = person.\("dob").asOpt[String],
        age = person.\("age").asOpt[String],
        connection_Type = person.\("connection_Type").asOpt[String],
        NINO = person.\("NINO").asOpt[String]
      )
    else null

  def getBusiness(result: JsLookupResult): Option[Business] =
    result.toOption map { businessData =>
      Business(
        (businessData \ "value_Fraud_Band").asOpt[String],
        (businessData \ "business_Type").asOpt[String],
        getAddress(businessData \ "address"),
        getContact(businessData \ "contact"),
        (businessData \ "connection_Type").asOpt[String],
        (businessData \ "VAT_Number").asOpt[String],
        (businessData \ "ct_Utr").asOpt[String],
        (businessData \ "employee_Number").asOpt[String]
      )
    }

  def getName(result: JsLookupResult): Option[Name] =
    result.toOption map { name =>
      Name(
        (name \ "forename").asOpt[String],
        (name \ "surname").asOpt[String],
        (name \ "middle_Name").asOpt[String],
        (name \ "alias").asOpt[String]
      )
    }

  def getContact(result: JsLookupResult): Option[Contact] =
    result.toOption map { contact =>
      Contact(
        (contact \ "landline_Number").asOpt[String],
        (contact \ "mobile_Number").asOpt[String],
        (contact \ "email_Address").asOpt[String]
      )
    }

  def getAddress(result: JsLookupResult): Option[Address] =
    result.toOption map { address =>
      Address(
        (address \ "address_Line_1").asOpt[String],
        (address \ "address_Line_2").asOpt[String],
        (address \ "address_Line_3").asOpt[String],
        (address \ "town_City").asOpt[String],
        (address \ "postcode").asOpt[String],
        (address \ "country").asOpt[String],
        (address \ "general_Location").asOpt[String]
      )
    }

  def getReporter(result: JsLookupResult): Option[Reporter] =
    result.toOption map { reporter =>
      Reporter(
        (reporter \ "forename").asOpt[String],
        (reporter \ "surname").asOpt[String],
        (reporter \ "telephone_Number").asOpt[String],
        (reporter \ "email_Address").asOpt[String],
        (reporter \ "memorable_Word").asOpt[String]
      )
    }

}
