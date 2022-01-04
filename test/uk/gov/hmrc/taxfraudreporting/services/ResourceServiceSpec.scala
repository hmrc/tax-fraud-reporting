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

import com.fasterxml.jackson.core.JsonParseException
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.test.Injecting

import java.io.IOException

class ResourceServiceSpec extends AnyFreeSpec with Matchers with GuiceOneAppPerSuite with Injecting {

  private lazy val resourceService: ResourceService = inject[ResourceService]

  private val json = Json.obj("foo" -> "bar")

  "a resource service" - {

    "must return the contents of a file as a string" in {
      resourceService.getFile("test.txt") mustEqual "foo = bar"
    }

    "must return the contents of a file as json" in {
      resourceService.getJson("test.json") mustEqual json
    }

    "must throw an exception when a file can't be found" in {

      an[IOException] mustBe thrownBy {
        resourceService.getFile("non-existent.txt")
      }
    }

    "must throw an exception when a file can't be parsed as json" in {

      a[JsonParseException] mustBe thrownBy {
        resourceService.getJson("test.txt")
      }
    }
  }
}
