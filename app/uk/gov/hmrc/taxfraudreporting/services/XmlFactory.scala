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

import play.api.{Configuration, Logger}
import uk.gov.hmrc.taxfraudreporting.models.FraudReport

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.language.postfixOps

@Singleton
class XmlFactory @Inject() (val configuration: Configuration) extends Configured("xmlFactory") {
  private val logger = Logger(this.getClass)
  logger.info("Initialising XML Factory.")

  private val rootElement = configured("rootElement")

  private def timestamp(dateTime: LocalDateTime) =
    dateTime.format(DateTimeFormatter ofPattern configured("timePattern"))

  def getOpening: String = s"""<?xml version="1.0" encoding="UTF-8"?><$rootElement>"""

  def getFileHeader(correlationID: UUID, dateTime: LocalDateTime, numOfReports: Long): String = {
    logger.info(s"Building header $correlationID")

    <header>
      <correlationId>{correlationID}</correlationId>
      <sendingSystem>{configured("sendingSystem")}</sendingSystem>
      <receivingSystem>{configured("receivingSystem")}</receivingSystem>
      <extractDateTime>{timestamp(dateTime)}</extractDateTime>
      <filename>{getFileName(dateTime)}</filename>
      <numReports>{numOfReports}</numReports>
      <fileVersion>{configured("version")}</fileVersion>
    </header> toString
  }

  private val valueFraudBands = List(25000, 100000, 500000, 1000000)

  def getReport(report: FraudReport, index: Long): String = {
    logger.info(s"Preparing fraud report #${index + 1}.")
    val FraudReport(fraudReportBody, submitted, _, _, id) = report

    val valueFraud     = fraudReportBody.valueFraud getOrElse 0L
    val valueFraudBand = valueFraudBands.count(_ <= valueFraud) + 1

    <report>
      <reportNumber>{index + 1}</reportNumber>
      <digitalID>{id}</digitalID>
      <submitted>{timestamp(submitted)}</submitted>
      <activityType>{fraudReportBody.activityType}</activityType>
      <informationSource>{fraudReportBody.informationSource}</informationSource>
      <nominals>{fraudReportBody.nominals map { _.toXml }}</nominals>
      <valueFraud>{valueFraud}</valueFraud>
      <valueFraudBand>{valueFraudBand}</valueFraudBand>
      <durationFraud>{fraudReportBody.durationFraud.orNull}</durationFraud>
      <howManyKnow>{fraudReportBody.howManyKnow.orNull}</howManyKnow>
      <additionalDetails>{fraudReportBody.additionalDetails}</additionalDetails>
      { fraudReportBody.reporter map { _.toXml } orNull }
      <supportingEvidence>{fraudReportBody.hasEvidence}</supportingEvidence>
      { fraudReportBody.evidenceDetails.map { value =>
        <evidenceDetails>{value}</evidenceDetails>
      }.orNull }
    </report> toString
  }

  def getClosing: String = s"</$rootElement>"

}
