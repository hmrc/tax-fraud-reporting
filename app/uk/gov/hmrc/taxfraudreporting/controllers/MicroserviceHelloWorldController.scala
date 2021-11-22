/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.taxfraudreporting.controllers

import org.micchon.playjsonxml.Xml
import org.xml.sax.InputSource
import play.api.Environment
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.io.StringReader
import javax.inject.{Inject, Singleton}
import javax.xml.XMLConstants
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Try

@Singleton
class MicroserviceHelloWorldController @Inject() (
  cc: ControllerComponents,
  env: Environment,
  ApiAction: ApiActionBuilder
) extends BackendController(cc) {

  def hello: Action[AnyContent] = Action.async {
    Future.successful(Ok("Hello world"))
  }

  def postReport: Action[JsValue] =
    ApiAction(parse.json) { implicit request =>
      val reportXml = <fraudReport>{Xml.toXml(request.body)}</fraudReport>
      val xmlString = s"""<?xml version="1.0"?>$reportXml"""
      val source    = new SAXSource(new InputSource(new StringReader(xmlString)))

      val schemaFactory = SchemaFactory newInstance XMLConstants.W3C_XML_SCHEMA_NS_URI
      val schemaFile    = env.classLoader getResourceAsStream "fraudReport.xsd"
      val schema        = schemaFactory newSchema new StreamSource(schemaFile)
      val validator     = schema newValidator

      Try(validator validate source).fold(
        exception => BadRequest(s"Could not validate fraud report against schema: ${exception.getMessage}"),
        _ => Ok(reportXml)
      )
    }

  def handleOptions: Action[AnyContent] = ApiAction {
    NoContent.withHeaders("Access-Control-Allow-Headers" -> "Content-type", "Accept" -> "application/json")
  }

}
