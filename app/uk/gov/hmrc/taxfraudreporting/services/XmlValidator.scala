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

package uk.gov.hmrc.taxfraudreporting.services

import org.xml.sax.InputSource
import play.api.Environment

import javax.xml.XMLConstants
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

abstract class XmlValidator(schemaFileName: String, environment: Environment) {
  private val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
  private val schemaFile    = environment.classLoader getResourceAsStream schemaFileName
  private val schema        = schemaFactory.newSchema(new StreamSource(schemaFile))
  private val validator     = schema.newValidator

  def validate(inputSource: InputSource): Unit =
    validator.validate(new SAXSource(inputSource))

}
