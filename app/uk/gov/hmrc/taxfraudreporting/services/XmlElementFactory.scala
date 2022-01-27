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

import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.taxfraudreporting.models.FraudReport
import uk.gov.hmrc.taxfraudreporting.xml.mapping.FraudReportMapper
import uk.gov.hmrc.taxfraudreporting.xml.models.Reporter

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}

@Singleton
case class XmlElementFactory @Inject() (servicesConfig: ServicesConfig) {
  private def configured(prop: String) = servicesConfig getString s"worker.file.$prop"

  private val timePattern = "dd/MM/yyyy HH:mm:ss"

  def getFileHeader(dateTime: LocalDateTime, numOfReports: Int): xml.Elem = {
    def timestamp(format: String) = dateTime.format(DateTimeFormatter ofPattern format)

    <fileHeader>
      <sending_System> {configured("sendingSystem")} </sending_System>
      <receiving_System> {configured("receivingSystem")} </receiving_System>
      <extract_Date_Time> {timestamp(timePattern)} </extract_Date_Time>
      <filename> {configured("nameFormat").format(timestamp("yyyyMMMddHHmm"))} </filename>
      <num_Reports> {numOfReports} </num_Reports>
      <file_Version> {configured("version")} </file_Version>
    </fileHeader>
  }

  private val valueFraudBands = List(25000, 100000, 500000, 1000000)

  def getReport(entity: (FraudReport, Long)): xml.Elem = {
    val (FraudReport(_, correlationId, body, submitted, _), reportNumber) = entity

    val timeStamp = submitted.format(DateTimeFormatter ofPattern timePattern)

    val valueFraud     = (body \ "value_Fraud").as[Long]
    val valueFraudBand = valueFraudBands.count(_ < valueFraud) + 1

    val additionalDetails  = (body \ "additional_Details").asOpt[String].orNull
    val reporter           = (body \ "reporter").asOpt[Reporter].orNull
    val supportingEvidence = (body \ "supporting_Evidence").as[Boolean]

    <report>
      <report_Number>{reportNumber + 1}</report_Number>
      <digital_ID>{correlationId}</digital_ID>
      <submitted>{timeStamp}</submitted>
      <activity_Type>{(body \ "activity_Type").as[String]}</activity_Type>
      {FraudReportMapper.getNominal(body).toXml}
      <value_Fraud>{valueFraud}</value_Fraud>
      <value_Fraud_Band>{valueFraudBand}</value_Fraud_Band>
      <duration_Fraud>{(body \ "duration_Fraud").as[String]}</duration_Fraud>
      <how_Many_Knew>{(body \ "how_Many_Knew").as[String]}</how_Many_Knew>
      <additional_Details>{additionalDetails}</additional_Details>
      <reporter>{reporter}</reporter>
      <supporting_Evidence>{supportingEvidence}</supporting_Evidence>
    </report>
  }

}
