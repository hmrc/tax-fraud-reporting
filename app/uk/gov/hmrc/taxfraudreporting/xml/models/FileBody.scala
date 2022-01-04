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

case class FileBody(
  report_Number: String,
  digital_ID: String,
  submitted: String,
  activity_Type: String,
  nominals: Nominals,
  value_Fraud: String,
  value_Fraud_Band: Option[String] = None,
  duration_Fraud: String,
  how_Many_Knew: String,
  additional_Details: Option[String] = None,
  reporter: Option[Reporter],
  supporting_Evidence: Option[Boolean] = None
) extends FraudReportXml {

  override def toXml: Elem =
    <report>
      <report_Number>{report_Number}</report_Number>
      <digital_ID>{digital_ID}</digital_ID>
      <submitted>{submitted}</submitted>
      <activity_Type>{activity_Type}</activity_Type>
      {nominals.toXml}
      <value_Fraud>{value_Fraud}</value_Fraud>
      {optionToXml(value_Fraud_Band, Some("value_Fraud_Band"))}
      <duration_Fraud>{duration_Fraud}</duration_Fraud>
      <how_Many_Knew>{how_Many_Knew}</how_Many_Knew>
      {optionToXml(additional_Details, Some("additional_Details"))}
      {optionToXml(reporter, "reporter")}
      {optionToXml(supporting_Evidence, Some("supporting_Evidence"))}
    </report>

}
