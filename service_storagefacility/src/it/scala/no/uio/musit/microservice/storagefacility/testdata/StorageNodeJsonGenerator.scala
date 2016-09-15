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

package no.uio.musit.microservice.storagefacility.testdata

import no.uio.musit.microservice.storagefacility.domain.storage.StorageNodeId
import play.api.libs.json.{JsValue, Json}

object StorageNodeJsonGenerator {

  val defaultAddress = "Foo gate 13, 1111 Bar, Norge"

  def organisationJson(name: String): JsValue = {
    Json.parse(
      s"""{
          |  "type" : "Organisation",
          |  "area" : 2000.5,
          |  "name" : "$name",
          |  "areaTo" : 2100,
          |  "groupRead" : "foo",
          |  "height" : 3,
          |  "heightTo" : 3.5,
          |  "address" : "$defaultAddress",
          |  "groupWrite" : "bar"
          |}
     """.stripMargin
    )
  }

  def storageUnitJson(name: String, partOf: StorageNodeId): JsValue = {
    Json.parse(
      s"""{
          |  "type" : "StorageUnit",
          |  "area" : 0.5,
          |  "name" : "$name",
          |  "areaTo" : 0.5,
          |  "groupRead" : "foo",
          |  "height" : 0.5,
          |  "isPartOf" : ${partOf.underlying},
          |  "heightTo" : 0.6,
          |  "groupWrite" : "bar"
          |}
       """.stripMargin
    )
  }

  def roomJson(name: String, partOf: StorageNodeId): JsValue = {
    Json.parse(
      s"""{
          |  "type" : "Room",
          |  "sikringRutineOgBeredskap" : false,
          |  "area" : 20.6,
          |  "name" : "$name",
          |  "areaTo" : 21,
          |  "sikringBrannsikring" : true,
          |  "bevarLuftfuktOgTemp" : true,
          |  "groupRead" : "foo",
          |  "sikringTyverisikring" : true,
          |  "height" : 2.5,
          |  "bevarLysforhold" : false,
          |  "sikringSkallsikring" : false,
          |  "isPartOf" : ${partOf.underlying},
          |  "heightTo" : 2.6,
          |  "sikringVannskaderisiko" : true,
          |  "bevarPrevantKons" : true,
          |  "groupWrite" : "bar"
          |}
       """.stripMargin
    )
  }

  def buildingJson(name: String, partOf: StorageNodeId): JsValue = {
    Json.parse(
      s"""{
          |  "type" : "Building",
          |  "area" : 200.5,
          |  "name" : "$name",
          |  "areaTo" : 210,
          |  "groupRead" : "foo",
          |  "height" : 3,
          |  "isPartOf" : ${partOf.underlying},
          |  "heightTo" : 3.5,
          |  "address" : "$defaultAddress",
          |  "groupWrite" : "bar"
          |}
       """.stripMargin
    )
  }


}
