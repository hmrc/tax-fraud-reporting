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

package uk.gov.hmrc.taxfraudreporting.connectors

import com.typesafe.config.ConfigFactory
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.taxfraudreporting.models.sdes.{FileAudit, FileChecksum, FileMetaData, SDESFileNotifyRequest}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class SDESConnectorSpec extends AnyWordSpec with Matchers with MockFactory with HttpSupport with ConnectorSpec {
  val (protocol, host, port) = ("http", "host", "123")
  val config                 = Configuration(ConfigFactory.parseString(s"""
                                 | microservice.services.sdes {
                                 |    host     = $host
                                 |    port     = $port
                                 |  }
                                 |  services {
                                 |   sdes {
                                 |       location = "sdes-stub"
                                 |      }
                                 |   }
                                 |
                                 |""".stripMargin))

  val connector = new SDESConnectorImpl(mockHttp, new ServicesConfig(config), config)
  val uuid      = UUID.randomUUID()

  val notifyRequest = SDESFileNotifyRequest(
    "fraud-reporting",
    FileMetaData(
      "tax-fraud-reporting",
      "file1.dat",
      s"http://localhost:8464/object-store/object/tax-fraud-reporting/file1.dat",
      FileChecksum(value = "hashValue"),
      2000,
      List()
    ),
    FileAudit(uuid.toString)
  )

  "SDESConnectorImpl" when {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    "handling requests to notify  SDES about the files generated" must {
      val expectedUrl = s"$protocol://$host:$port/sdes-stub/notification/fileready"

      behave like connectorBehaviour(
        mockPost(expectedUrl, Seq.empty, notifyRequest)(_),
        () => connector.notify(notifyRequest)
      )

    }
  }
}
