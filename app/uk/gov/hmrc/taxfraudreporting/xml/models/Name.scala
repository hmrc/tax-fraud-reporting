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

case class Name(
  forename: Option[String] = None,
  surname: Option[String] = None,
  middle_Name: Option[String] = None,
  alias: Option[String] = None
) extends FraudReportXml {

  override def toXml: Elem =
    <name>
      {optionToXml(forename, Some("forename"))}
      {optionToXml(surname, Some("surname"))}
      {optionToXml(middle_Name, Some("middle_Name"))}
      {optionToXml(alias, Some("alias"))}
    </name>

}
