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

package uk.gov.hmrc.taxfraudreporting.xml.util

import scala.language.postfixOps
import scala.xml.Utility._
import scala.xml.{Elem, XML}

trait FraudReportXml {

  def toXml: Elem

  def optionToXml[T](opt: Option[T], tag: Option[String]): Elem =
    (opt, tag) match {
      case (Some(v), Some(t)) =>
        if (v.toString.trim.nonEmpty)
          <xml>{v.toString}</xml>.copy(label = t)
        else null
      case (Some(v: FraudReportXml), _) => v.toXml
      case _                            => null
    }

  def optionToXml(opt: Option[FraudReportXml], tag: String): Elem =
    opt map { _.toXml.copy(label = tag) } orNull

  def optionToXml(opt: Option[FraudReportXml]): Elem =
    opt map { _.toXml } orNull

  def optionToXml(opt: Option[Seq[FraudReportXml]]): Seq[Elem] =
    opt map { _.map(_.toXml) } orNull

  def toStringDoc: String = {
    val w = new java.io.StringWriter
    XML.write(w, trim(toXml), "UTF-8", xmlDecl = true, null)
    w.toString
  }

  def toPrettyPrintString: String = {
    val p = new scala.xml.PrettyPrinter(80, 4)
    p.format(toXml)
  }

}
