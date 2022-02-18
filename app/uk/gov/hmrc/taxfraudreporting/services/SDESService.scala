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

import cats.data.EitherT
import cats.instances.future.catsStdInstancesForFuture
import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.taxfraudreporting.connectors.SDESConnector
import uk.gov.hmrc.taxfraudreporting.models.Error
import uk.gov.hmrc.taxfraudreporting.models.sdes.SDESFileNotifyRequest

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[SDESServiceImpl])
trait SDESService {

  def fileNotify(fileNotifyRequest: SDESFileNotifyRequest)(implicit hc: HeaderCarrier): EitherT[Future, Error, Unit]
}

@Singleton
class SDESServiceImpl @Inject() (sdesConnector: SDESConnector)(implicit ec: ExecutionContext)
    extends SDESService with Logging {

  val message: String = "Call to notify SDES came back with status::"

  override def fileNotify(
    fileNotifyRequest: SDESFileNotifyRequest
  )(implicit hc: HeaderCarrier): EitherT[Future, Error, Unit] =
    sdesConnector
      .notify(fileNotifyRequest)
      .subflatMap { response =>
        response.status match {
          case NO_CONTENT =>
            logger.info(
              s"SDES has been notified of file :: ${fileNotifyRequest.file.name}  with correlationId::${fileNotifyRequest.audit.correlationID}"
            )
            Right(())
          case rest => Left(Error(s"$message $rest, ${response.body}"))
        }

      }

}
