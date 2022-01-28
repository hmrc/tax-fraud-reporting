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

package integration

import org.scalacheck.Shrink.shrinkAny
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.Logger
import shared.GenDriven
import uk.gov.hmrc.taxfraudreporting.services.XmlFactory

import java.io.StringReader
import java.time.LocalDateTime
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

class XMLFactorySpec extends IntegrationSpecCommonBase with GenDriven {
  private val logger = Logger(this.getClass)

  "XML Factory" should {
    val xmlFactory = injector.instanceOf[XmlFactory]

    "generate a valid EVI document from valid fraud report data" in {
      forAll(listsOfFraudReports) { listOfFraudReports =>
        val headerString = xmlFactory.getFileHeader(LocalDateTime.now(), listOfFraudReports.size)

        val reportString =
          listOfFraudReports.zipWithIndex flatMap {
            case (report, index) => xmlFactory.getReport(report, index)
          }

        val eviDocStr =
          xmlFactory.getOpening ++
            headerString ++
            reportString ++
            xmlFactory.getClosing

        logger.info(eviDocStr)

        val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        val schemaFile    = getClass.getClassLoader getResourceAsStream "EVI_BDApp_doc.xsd"
        val schema        = schemaFactory.newSchema(new StreamSource(schemaFile))
        val validator     = schema.newValidator

        validator validate new StreamSource(new StringReader(eviDocStr))
      }
    }
  }
}
