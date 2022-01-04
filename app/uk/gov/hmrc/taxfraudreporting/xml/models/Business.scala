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

package uk.gov.hmrc.taxfraudreporting.xml.models

import uk.gov.hmrc.taxfraudreporting.xml.util.FraudReportXml

import scala.xml.Elem

case class Business(
  business_Name: Option[String] = None,
  business_Type: Option[String] = None,
  address: Option[Address] = None,
  contact: Option[Contact] = None,
  connection_Type: Option[String] = None,
  VAT_Number: Option[String] = None,
  ct_Utr: Option[String] = None,
  employee_Number: Option[String] = None
) extends FraudReportXml {

  override def toXml: Elem =
    <business>
      {optionToXml(business_Name, Some("business_Name"))}
      {optionToXml(business_Type, Some("business_Type"))}
      {optionToXml(address)}
      {optionToXml(contact)}
      {optionToXml(connection_Type, Some("connection_Type"))}
      {optionToXml(VAT_Number, Some("VAT_Number"))}
      {optionToXml(ct_Utr, Some("ct_Utr"))}
      {optionToXml(employee_Number, Some("employee_Number"))}
    </business>

}
