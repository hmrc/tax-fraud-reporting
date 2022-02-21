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

package integration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import play.api.Logger

import java.net.ServerSocket

trait WiremockSupport extends BeforeAndAfterEach with BeforeAndAfterAll {
  this: Suite =>
  private val logger = Logger(this.getClass)
  val socket         = new ServerSocket(0)
  val host           = "localhost"

  val wiremockPort: Int = {
    val socket   = new ServerSocket(0)
    val freePort = socket.getLocalPort
    socket.close()
    freePort
  }

  val wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(wiremockPort))

  def startWiremock(): Unit = {
    logger.info(s"Starting up wiremock server on $host:$wiremockPort")
    wireMockServer.start()
    WireMock.configureFor(host, wiremockPort)
  }

  def stopWiremock(): Unit = wireMockServer.stop()

  def resetWiremock(): Unit = WireMock.reset()

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
  }

  override protected def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }

  override protected def beforeEach(): Unit = resetWiremock()

  def stubPost(url: String, requestJson: String, status: Integer): StubMapping =
    stubFor(
      post(urlMatching(url))
        .withRequestBody(equalToJson(requestJson))
        .willReturn(aResponse().withStatus(status))
    )

  def stubPostWithResponse(url: String, requestJson: String, status: Integer, response: String): StubMapping =
    stubFor(
      post(urlMatching(url))
        .withRequestBody(equalToJson(requestJson))
        .willReturn(aResponse().withStatus(status).withBody(response))
    )

}
