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

case class Address(
  address_Line_1: Option[String] = None,
  address_Line_2: Option[String] = None,
  address_Line_3: Option[String] = None,
  town_City: Option[String] = None,
  postcode: Option[String] = None,
  country: Option[String] = None,
  general_Location: Option[String] = None
) extends FraudReportXml {

  override def toXml: Elem =
    <address>
      {optionToXml(address_Line_1, Some("address_Line_1"))}
      {optionToXml(address_Line_2, Some("address_Line_2"))}
      {optionToXml(address_Line_3, Some("address_Line_3"))}
      {optionToXml(town_City, Some("town_City"))}
      {optionToXml(postcode, Some("postcode"))}
      {optionToXml(country, Some("country"))}
      {optionToXml(general_Location, Some("general_Location"))}
    </address>

}
