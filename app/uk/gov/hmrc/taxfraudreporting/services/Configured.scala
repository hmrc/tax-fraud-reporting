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

import play.api.Configuration

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

abstract class Configured(service: String) {
  val configuration: Configuration
  val configured: Map[String, String] = configuration.get[Map[String, String]](s"services.$service")

  def getFileName(datetime: LocalDateTime): String = {
    val fileNameFormat = configuration.get[String]("services.fileNameFormat")
    val dateTimeFormat = configuration.get[String]("services.dateTimeFormat")

    val timestamp = datetime.format(DateTimeFormatter ofPattern dateTimeFormat)
    fileNameFormat.format(timestamp)
  }

}
