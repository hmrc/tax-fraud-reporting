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
      <correlation_Id>{correlationID}</correlation_Id>
      <sending_System>{configured("sendingSystem")}</sending_System>
      <receiving_System>{configured("receivingSystem")}</receiving_System>
      <extract_Date_Time>{timestamp(dateTime)}</extract_Date_Time>
      <filename>{getFileName(dateTime)}</filename>
      <num_Reports>{numOfReports}</num_Reports>
      <file_Version>{configured("version")}</file_Version>
    </header> toString
  }

  private val valueFraudBands = List(25000, 100000, 500000, 1000000)

  def getReport(report: FraudReport, index: Long): String = {
    logger.info(s"Preparing fraud report #${index + 1}.")
    val FraudReport(fraudReportBody, submitted, _, _, id) = report

    val valueFraud     = fraudReportBody.valueFraud getOrElse 0L
    val valueFraudBand = valueFraudBands.count(_ <= valueFraud) + 1

    <report>
      <report_Number>{index + 1}</report_Number>
      <digital_ID>{id}</digital_ID>
      <submitted>{timestamp(submitted)}</submitted>
      <activity_Type>{fraudReportBody.activityType}</activity_Type>
      <informationSource>{fraudReportBody.informationSource}</informationSource>
      <nominals>{fraudReportBody.nominals map { _.toXml }}</nominals>
      <value_Fraud>{valueFraud}</value_Fraud>
      <value_Fraud_Band>{valueFraudBand}</value_Fraud_Band>
      <duration_Fraud>{fraudReportBody.durationFraud.orNull}</duration_Fraud>
      <how_Many_Know>{fraudReportBody.howManyKnow.orNull}</how_Many_Know>
      <additional_Details>{fraudReportBody.additionalDetails}</additional_Details>{
      fraudReportBody.reporter map { _.toXml } orNull
    }<supporting_Evidence>{fraudReportBody.hasEvidence}</supporting_Evidence>
    </report> toString
  }

  def getClosing: String = s"</$rootElement>"

}
