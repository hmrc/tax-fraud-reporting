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

case class FileHeader(
  sending_System: String,
  receiving_System: String,
  extract_Date_Time: String,
  filename: String,
  num_Reports: String,
  file_Version: String
) extends FraudReportXml {

  override def toXml: Elem =
    <fileHeader>
      <sending_System> {sending_System} </sending_System>
      <receiving_System> {receiving_System} </receiving_System>
      <extract_Date_Time> {extract_Date_Time} </extract_Date_Time>
      <filename> {filename} </filename>
      <num_Reports> {num_Reports} </num_Reports>
      <file_Version> {file_Version} </file_Version>
    </fileHeader>

}
