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

import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.{Configuration, Logging}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.taxfraudreporting.models.sdes.SDESFileNotifyRequest

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[SDESServiceImpl])
trait SDESService {

  def fileNotify(fileNotifyRequest: SDESFileNotifyRequest)(implicit hc: HeaderCarrier): Future[Unit]
}

@Singleton
class SDESServiceImpl @Inject() (http: HttpClient, servicesConfig: ServicesConfig, config: Configuration)(implicit
  ec: ExecutionContext
) extends SDESService with Logging {

  private val baseUrl: String             = servicesConfig.baseUrl("sdes")
  private val apiLocation: Option[String] = Some(config.get[String]("services.sdes.location")).filter(_.nonEmpty)

  private val sdesUrl: String =
    List(Some(baseUrl), apiLocation, Some("notification"), Some("fileready")).flatten.mkString("/")

  private val clientId: String                    = config.get[String]("services.sdes.client-id")
  private val extraHeaders: Seq[(String, String)] = Seq("x-client-id" -> clientId)

  override def fileNotify(fileNotifyRequest: SDESFileNotifyRequest)(implicit hc: HeaderCarrier): Future[Unit] = {
    logger.debug(s"SDES notification request: ${Json.stringify(Json.toJson(fileNotifyRequest))}")
    http.POST[SDESFileNotifyRequest, HttpResponse](sdesUrl, fileNotifyRequest, extraHeaders).flatMap {
      response =>
        response.status match {
          case NO_CONTENT =>
            logger.info(
              s"SDES has been notified of file :: ${fileNotifyRequest.file.name}  with correlationId::${fileNotifyRequest.audit.correlationID}"
            )
            Future.successful(())
          case status =>
            val e = new Exception(s"Exception in notifying SDES. Received http status: $status body: ${response.body}")
            logger.error(
              s"Received a non 204 status from SDES when notified about file :: ${fileNotifyRequest.file.name}  with correlationId::${fileNotifyRequest.audit.correlationID}.",
              e
            )
            Future.failed(e)
        }
    }
  }

}
