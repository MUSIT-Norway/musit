/*
 * MUSIT is a museum database to archive natural and cultural history data.
 * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package utils.testhelpers

import no.uio.musit.models.StorageNodeDatabaseId
import play.api.libs.json.{JsValue, Json}

object StorageNodeJsonGenerator {

  val defaultAddress = "Foo gate 13, 1111 Bar, Norge"

  def envReqJson(cleaning: String = "Keep it clean!") = {
    s"""{
        |  "temperature" : {
        |    "base" : 20,
        |    "tolerance" : 25
        |  },
        |  "relativeHumidity" : {
        |    "base" : 60.7,
        |    "tolerance" : 70
        |  },
        |  "hypoxicAir" : {
        |    "base" : 12,
        |    "tolerance" : 20
        |  },
        |  "cleaning" : "$cleaning",
        |  "lightingCondition" : "Dempet belysning",
        |  "comment" : "Kommentar for environment requirement."
        |}
    """.stripMargin
  }

  def rootJson(name: String): JsValue =
    Json.parse(s"""{ "name" : "$name", "type": "Root" }""")

  def rootLoan(name: String): JsValue =
    Json.parse(s"""{ "name" : "$name", "type": "RootLoan" }""")

  def organisationJson(
      name: String,
      partOf: Option[StorageNodeDatabaseId] = None
  ): JsValue = {
    val pof = partOf.map(p => s""""isPartOf" : ${p.underlying},""")
    Json.parse(
      s"""{
          |  "type" : "Organisation",
          |  "name" : "$name",
          |  "area" : 2000.5,
          |  "areaTo" : 2100,
          |  "height" : 3,
          |  "heightTo" : 3.5,
          |  ${pof.getOrElse("")}
          |  "groupRead" : "foo",
          |  "groupWrite" : "bar",
          |  "address" : "$defaultAddress"
          |}
     """.stripMargin
    )
  }

  def storageUnitJson(name: String, partOf: StorageNodeDatabaseId): JsValue = {
    Json.parse(
      s"""{
          |  "type" : "StorageUnit",
          |  "name" : "$name",
          |  "area" : 0.5,
          |  "areaTo" : 0.5,
          |  "height" : 0.5,
          |  "heightTo" : 0.6,
          |  "isPartOf" : ${partOf.underlying},
          |  "groupRead" : "foo",
          |  "groupWrite" : "bar",
          |  "environmentRequirement" : ${envReqJson()}
          |}
       """.stripMargin
    )
  }

  def roomJson(name: String, partOf: Option[StorageNodeDatabaseId] = None): JsValue = {
    val pof = partOf.map(p => s""""isPartOf" : ${p.underlying},""")
    Json.parse(
      s"""{
          |  "type" : "Room",
          |  "name" : "$name",
          |  "area" : 20.5,
          |  "areaTo" : 21,
          |  "height" : 2,
          |  "heightTo" : 2.6,
          |  ${pof.getOrElse("")}
          |  "groupRead" : "foo",
          |  "groupWrite" : "bar",
          |  "securityAssessment" : {
          |    "perimeter" : true,
          |    "theftProtection" : true,
          |    "fireProtection" : false,
          |    "waterDamage" : true,
          |    "routinesAndContingencyPlan" : true
          |  },
          |  "environmentAssessment" : {
          |    "relativeHumidity" : true,
          |    "temperature" : true,
          |    "lightingCondition" : true,
          |    "preventiveConservation" : false
          |  },
          |  "environmentRequirement" : ${envReqJson()}
          |}
       """.stripMargin
    )
  }

  def buildingJson(name: String, partOf: StorageNodeDatabaseId): JsValue = {
    Json.parse(
      s"""{
          |  "type" : "Building",
          |  "name" : "$name",
          |  "area" : 200,
          |  "areaTo" : 210.0,
          |  "height" : 3.1,
          |  "heightTo" : 3.5,
          |  "isPartOf" : ${partOf.underlying},
          |  "groupRead" : "foo",
          |  "groupWrite" : "bar",
          |  "environmentRequirement" : ${envReqJson()},
          |  "address" : "$defaultAddress"
          |}
       """.stripMargin
    )
  }

}
