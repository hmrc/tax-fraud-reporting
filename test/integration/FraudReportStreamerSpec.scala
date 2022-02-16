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

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import org.scalacheck.Shrink.shrinkAny
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.Logger
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import shared.GenDriven
import uk.gov.hmrc.taxfraudreporting.repositories.FraudReportRepositoryImpl
import uk.gov.hmrc.taxfraudreporting.services.FraudReportStreamer

import java.nio.charset.Charset
import java.time.LocalDateTime
import java.util.UUID
import scala.language.postfixOps
import scala.xml.XML

class FraudReportStreamerSpec extends IntegrationSpecCommonBase with GenDriven {
  private val logger = Logger(getClass)

  override def afterEach(): Unit = {
    super.afterEach()
    await(reportRepo.collection.drop().toFuture)
  }

  private implicit val actorSystem: ActorSystem = injector.instanceOf[ActorSystem]

  private val reportRepo = injector.instanceOf[FraudReportRepositoryImpl]
  private val streamer   = injector.instanceOf[FraudReportStreamer]

  "The EVI document produced by the FraudReportStreamer" should {
    "include all unprocessed and exclude all processed fraud reports" in {
      forAll(listsOfFraudReports) { listOfFraudReports =>
        val result = reportRepo.collection.insertMany(listOfFraudReports).toFuture.futureValue
        logger.info("Inserted IDs:")
        result.getInsertedIds.values forEach { id =>
          logger.info(id.toString)
        }

        val correlationID = UUID.randomUUID()
        val source        = streamer.stream(correlationID, LocalDateTime.now())
        val sink          = Sink.reduce[ByteString](_ ++ _)

        val byteString  = source runWith sink futureValue
        val xmlString   = byteString decodeString Charset.defaultCharset()
        val reportNodes = XML.loadString(xmlString) \\ "report"

        val reportIds = reportNodes map { node => (node \ "digital_ID").head.text }

        listOfFraudReports foreach { fraudReport =>
          val reportIncluded = reportIds contains fraudReport._id.toString
          assert(reportIncluded ^ fraudReport.isProcessed)
        }
      }
    }
  }
}
