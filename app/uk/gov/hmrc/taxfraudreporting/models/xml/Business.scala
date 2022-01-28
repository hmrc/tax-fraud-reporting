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

package uk.gov.hmrc.taxfraudreporting.models.xml

import play.api.libs.json.{Json, OFormat}

final case class Business(
  businessName: Option[String] = None,
  businessType: Option[String] = None,
  address: Option[Address] = None,
  contact: Option[Contact] = None,
  businessVatNo: Option[String] = None,
  ctUtr: Option[String] = None,
  employeeRefNo: Option[String] = None,
  connectionType: String
) extends FraudReportXml {

  def toXml: xml.Elem =
    <business>
      {optionToXml(businessName, "businessName")}
      {optionToXml(businessType, "businessType")}
      {optionToXml(address)}
      {optionToXml(contact)}
      {optionToXml(businessVatNo, "businessVATNo")}
      {optionToXml(ctUtr, "ctUTR")}
      {optionToXml(employeeRefNo, "subjectPAYERef")}
      <reporterConn>{connectionType}</reporterConn>
    </business>

}

object Business {
  implicit val format: OFormat[Business] = Json.format
}
