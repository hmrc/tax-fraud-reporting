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

package uk.gov.hmrc.taxfraudreporting.xml.mapping

import play.api.libs.json.{JsValue, Json}

import scala.xml.{Node, Utility}

trait TestData {

  lazy val fraudReportBody: JsValue = Json.parse {
    s"""
       |{
       |	"activity_Type": "Fraud related to furlough",
       |	"report_Number": "123",
       |	"digital_ID": "XXX",
       |	"person": [
       |		{
       |			"name": {
       |				"forename": "Adam",
       |				"surname": "Umerji",
       |				"middle_Name": "middleName",
       |				"alias": "AdamAilas"
       |			},
       |			"address": {
       |				"address_Line_1": "London 111",
       |				"address_Line_2": "Line 2",
       |				"address_Line_3": "Line 3",
       |				"town_City": "London",
       |				"postcode": "NW1 11",
       |				"country": "UK",
       |				"general_Location": "general Location"
       |			},
       |			"contact": {
       |				"landline_Number": "9809809801",
       |				"mobile_Number": "9809809801",
       |				"email_Address": "email@mail.com"
       |			},
       |			"dob": "123",
       |			"age": "98",
       |			"connection_Type": "Partner",
       |			"NINO": "NINO"
       |		}
       |	],
       |	"business": {
       |		"business_Name": "businessName",
       |		"business_Type": "Retails",
       |		"address": {
       |			"address_Line_1": "111",
       |			"address_Line_2": "222",
       |			"address_Line_3": "333",
       |			"town_City": "",
       |			"postcode": "EH12 9JE",
       |			"country": "",
       |			"general_Location": ""
       |		},
       |		"contact": {
       |			"landlineNumber": "1111111",
       |			"mobileNumber": "22222222",
       |			"emailAddress": "alex@tom.com"
       |		},
       |		"VAT_Number": "XXNNNNNNNNN",
       |		"ct_Utr": "1234567890",
       |		"employee_Number": "123"
       |	},
       |	"value_Fraud": "12333",
       |	"value_Fraud_Band": 4,
       |	"duration_Fraud": "Over 5 years ago",
       |	"how_Many_Knew": "6",
       |	"additional_Details": "You have 250 words",
       |	"reporter": {
       |		"forename": "reporter Name",
       |		"surname": "reporter surname",
       |		"telephone_Number": "123456789",
       |		"email_Address": "email@mail.com",
       |		"memorable_Word": "test word"
       |	},
       |	"supporting_Evidence": true,
       |	"submitted": "2021-08-20T14:29:10.792"
       |}
         """.stripMargin
  }

  lazy val mandatoryFieldsFraudReportBody: JsValue = Json.parse {
    s"""
       |{
       |	"activity_Type": "Fraud related to furlough",
       |	"report_Number": "123",
       | 	"duration_Fraud": "Over 5 years ago",
       |	"digital_ID": "XXX",
       |	"business": {},
       |	"value_Fraud": "12333",
       |	"how_Many_Knew": "6"
       |}
         """.stripMargin
  }

  lazy val expectedFraudReportXml: String = s"$declaration\n<reports>$fraudReportXml</reports>"

  private lazy val declaration = "<?xml version='1.0' encoding='UTF-8'?>"

  private lazy val fraudReportXml: Node = Utility.trim(<report>
        <report_Number>XYPR0000000001</report_Number>
        <digital_ID>XYPR0000000001</digital_ID>
        <submitted>01/01/2017 22:20:30</submitted>
        <activity_Type>Fraud related to furlough</activity_Type>
        <nominals>
          <nominal>
            <person>
              <name>
                <forename>Adam</forename>
                <surname>Umerji</surname>
                <middle_Name>middleName</middle_Name>
                <alias>AdamAilas</alias>
              </name>
              <address>
                <address_Line_1>London 111</address_Line_1>
                <address_Line_2>Line 2</address_Line_2>
                <address_Line_3>Line 3</address_Line_3>
                <town_City>London</town_City>
                <postcode>NW1 11</postcode>
                <country>UK</country>
                <general_Location>general Location</general_Location>
              </address>
              <contact>
                <landline_Number>9809809801</landline_Number>
                <mobile_Number>9809809801</mobile_Number>
                <email_Address>email@mail.com</email_Address>
              </contact>
              <dob>123</dob>
              <age>98</age>
              <connection_Type>Partner</connection_Type>
              <NINO>NINO</NINO>
            </person>
            <business>
              <business_Type>Retails</business_Type>
              <address>
                <address_Line_1>111</address_Line_1>
                <address_Line_2>222</address_Line_2>
                <address_Line_3>333</address_Line_3>
                <postcode>EH12 9JE</postcode>
              </address>
              <contact/>
              <VAT_Number>XXNNNNNNNNN</VAT_Number>
              <ct_Utr>1234567890</ct_Utr>
              <employee_Number>123</employee_Number>
            </business>
          </nominal>
        </nominals>
        <value_Fraud>12333</value_Fraud>
        <duration_Fraud>Over 5 years ago</duration_Fraud>
        <how_Many_Knew>6</how_Many_Knew>
        <additional_Details>You have 250 words</additional_Details>
        <reporter>
          <forename>reporter Name</forename>
          <surname>reporter surname</surname>
          <telephone_Number>123456789</telephone_Number>
          <email_Address>email@mail.com</email_Address>
          <memorable_Word>test word</memorable_Word>
        </reporter>
        <supporting_Evidence>true</supporting_Evidence>
      </report>)

  lazy val expectedFraudReportXmlForMandatoryFields: String =
    s"$declaration\n<reports>$fraudReportXmlForMandatoryFields</reports>"

  private lazy val fraudReportXmlForMandatoryFields: Node = Utility.trim(<report>
        <report_Number>XYPR0000000001</report_Number>
        <digital_ID>XYPR0000000001</digital_ID>
        <submitted>01/01/2017 22:20:30</submitted>
        <activity_Type>Fraud related to furlough</activity_Type>
        <nominals>
          <nominal>
            <business/>
          </nominal>
        </nominals>
        <value_Fraud>12333</value_Fraud>
        <duration_Fraud>Over 5 years ago</duration_Fraud>
        <how_Many_Knew>6</how_Many_Knew>
      </report>)

  lazy val expectedFraudReportXmlForCombinedRecords: String =
    s"$declaration\n<reports>$fraudReportXmlForMandatoryFields$fraudReportXmlForMandatoryFields</reports>"

}
