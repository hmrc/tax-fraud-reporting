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

final case class Address(
  addressLine1: Option[String] = None,
  addressLine2: Option[String] = None,
  addressLine3: Option[String] = None,
  townCity: Option[String] = None,
  postcode: Option[String] = None,
  country: Option[String] = None
) extends FraudReportXml {

  def toXml: xml.Elem =
    <address>
      {optionToXml(addressLine1, "address_Line_1")}
      {optionToXml(addressLine2, "address_Line_2")}
      {optionToXml(addressLine3, "address_Line_3")}
      {optionToXml(townCity, "town_City")}
      {optionToXml(postcode, "postcode")}
      {optionToXml(country, "country")}
    </address>

}

object Address {
  implicit val format: OFormat[Address] = Json.format
}
