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

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.ByteString
import uk.gov.hmrc.taxfraudreporting.repositories.FraudReportRepository

import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.language.postfixOps

@Singleton
class FraudReportStreamer @Inject() (fraudReportRepository: FraudReportRepository, xmlFactory: XmlFactory) {

  def stream(correlationID: UUID, extractTime: LocalDateTime): Source[ByteString, NotUsed] = {
    val opening = Source.single(xmlFactory.getOpening)

    val header =
      Source.fromPublisher(fraudReportRepository.countUnprocessed)
        .map(xmlFactory.getFileHeader(correlationID, extractTime, _))

    val reports =
      Source.fromPublisher(fraudReportRepository.listUnprocessed)
        .zipWithIndex
        .map(xmlFactory.getReport _ tupled)

    val closing = Source.single(xmlFactory.getClosing)

    opening concat
      header concat
      reports concat
      closing map (xml => ByteString.apply(xml, StandardCharsets.ISO_8859_1))

  }

}
