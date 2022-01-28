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

package uk.gov.hmrc.taxfraudreporting.models.xml

import scala.language.postfixOps

trait FraudReportXml {

  def toXml: xml.Elem

  def optionToXml[T](opt: Option[T], tag: String): xml.Elem =
    opt match {
      case Some(v) if v.toString.trim.nonEmpty =>
        <xml>{v.toString}</xml>.copy(label = tag)
      case _ => null
    }

  def optionToXml(opt: Option[FraudReportXml]): xml.Elem =
    opt map { _.toXml } orNull

}
