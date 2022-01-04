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

case class Person(
  name: Option[Name] = None,
  address: Option[Address] = None,
  contact: Option[Contact] = None,
  dob: Option[String] = None,
  age: Option[String] = None,
  connection_Type: Option[String] = None,
  NINO: Option[String] = None
) extends FraudReportXml {

  override def toXml: Elem =
    <person>
      {optionToXml(name)}
      {optionToXml(address)}
      {optionToXml(contact)}
      {optionToXml(dob, Some("dob"))}
      {optionToXml(age, Some("age"))}
      {optionToXml(connection_Type, Some("connection_Type"))}
      {optionToXml(NINO, Some("NINO"))}
    </person>

}
