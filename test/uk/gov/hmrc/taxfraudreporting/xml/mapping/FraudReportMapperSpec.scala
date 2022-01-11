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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.taxfraudreporting.models.{FraudReference, FraudReport}
import uk.gov.hmrc.taxfraudreporting.xml.models.FraudReports

import java.time.{LocalDateTime, Month}

class FraudReportMapperSpec extends AnyWordSpec with Matchers with TestData {

  val firstReport: FraudReport = FraudReport(
    _id = FraudReference(1),
    correlationId = "XXX",
    body = fraudReportBody,
    submitted = LocalDateTime.of(2017, Month.JANUARY, 1, 22, 20, 30)
  )

  val secondReport: FraudReport = FraudReport(
    _id = FraudReference(1),
    correlationId = "XXX",
    body = mandatoryFieldsFraudReportBody,
    submitted = LocalDateTime.of(2017, Month.JANUARY, 1, 22, 20, 30)
  )

  "fraud report mapping" should {
    "return xml records for the given json data" in {
      val report: FraudReports = FraudReportMapper.getXmlFraudReports(Seq(firstReport))
      val stringBuffer         = new StringBuffer(report.toStringDoc)
      stringBuffer.delete(
        stringBuffer.indexOf("<fileHeader>"),
        stringBuffer.indexOf("</fileHeader>") + "</fileHeader>".length
      ).toString mustBe expectedFraudReportXml
    }

    "return xml records for the given json data - only provided the mandatory values" in {
      val report: FraudReports = FraudReportMapper.getXmlFraudReports(Seq(secondReport))
      val stringBuffer         = new StringBuffer(report.toStringDoc)
      stringBuffer.delete(
        stringBuffer.indexOf("<fileHeader>"),
        stringBuffer.indexOf("</fileHeader>") + "</fileHeader>".length
      ).toString mustBe expectedFraudReportXmlForMandatoryFields
    }

    "return combined xml records of two fraud report" in {
      val report: FraudReports = FraudReportMapper.getXmlFraudReports(Seq(secondReport, secondReport))
      val stringBuffer         = new StringBuffer(report.toStringDoc)
      stringBuffer.delete(
        stringBuffer.indexOf("<fileHeader>"),
        stringBuffer.indexOf("</fileHeader>") + "</fileHeader>".length
      ).toString mustBe expectedFraudReportXmlForCombinedRecords
    }
  }
}
