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

package uk.gov.hmrc.taxfraudreporting.controllers

import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import play.api.{Configuration, Logging}
import uk.gov.hmrc.objectstore.client.Path
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.taxfraudreporting.models.sdes.CallBackNotification
import uk.gov.hmrc.taxfraudreporting.models.sdes.NotificationStatus._
import uk.gov.hmrc.taxfraudreporting.repositories.FraudReportRepository

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class SDESCallbackController @Inject() (
  objectStoreClient: PlayObjectStoreClient,
  fraudReportRepository: FraudReportRepository,
  cc: ControllerComponents,
  configuration: Configuration
)(implicit executionContext: ExecutionContext)
    extends BackendController(cc) with Logging {

  private val path = Path Directory configuration.get[String]("services.objectStorageWorker.path")

  def callback: Action[JsValue] = Action(parse.json).async { implicit request =>
    Json.fromJson[CallBackNotification](request.body) match {
      case JsSuccess(callbackNotification, _) =>
        logger.info(
          s"Received SDES callback for file: ${callbackNotification.filename}, " +
            s"with correlationId : ${callbackNotification.correlationID} and status : ${callbackNotification.notification}"
        )
        import callbackNotification._
        notification match {
          case FileReady | FileReceived => Future.successful(Ok)
          case FileProcessingFailure =>
            objectStoreClient.deleteObject(path file filename).map(_ => Ok)
          case FileProcessed =>
            for {
              _ <- objectStoreClient.deleteObject(path file filename)
              _ <- fraudReportRepository.updateUnprocessed(UUID.fromString(correlationID))
            } yield Ok
        }

      case JsError(err) =>
        logger.warn(s"Failed to parse the SDES callback notification with error:: $err")
        Future.successful(BadRequest)
    }

  }

}
