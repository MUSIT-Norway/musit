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

package no.uio.musit.microservice.storagefacility.test

object EventJsonGenerator {

  def baseEventJson(eventType: String, regDay: Int) = {
    s""""doneDate" : "2016-09-${regDay}T09:17:46+02:00",
        |"doneBy" : 12,
        |"eventType" : "$eventType"""".stripMargin
  }

  def controlJson(regDay: Int) = {
    s"""{
        |  ${baseEventJson("Control", regDay)},
        |  "temperature" : ${ctrlSubFromToJson("temperature", ok = false)},
        |  "alcohol": ${ctrlSubFromToJson("alcohol", ok = false)},
        |  "cleaning": ${ctrlSubStringJson("cleaning", ok = false)},
        |  "pest": ${ctrlSubPestJson(ok = false)}
        |}""".stripMargin
  }

  def observationJson(regDay: Int) = {
    s"""{
        |  ${baseEventJson("Observation", regDay)},
        |  "temperature" : ${obsFromToJson("temperature")},
        |  "alcohol": ${obsFromToJson("alcohol")},
        |  "cleaning": ${obsStringJson("cleaning")},
        |  "pest": ${obsPestJson}
        |}""".stripMargin
  }

  def observation(propName: String, ok: Boolean)(obsJson: (String) => String) = {
    if (!ok) {
      s""""observation" : ${obsJson(propName)}"""
    } else {
      ""
    }
  }

  def ctrlSubStringJson(propName: String, ok: Boolean = true) = {
    val maybeMotivates = observation(propName, ok)(obsStringJson)
    s"""{
        |  "ok" : $ok,
        |  $maybeMotivates
        |}""".stripMargin
  }

  def ctrlSubFromToJson(propName: String, ok: Boolean = true) = {
    val maybeObservation = observation(propName, ok)(obsFromToJson)
    s"""{
        |  "ok" : $ok,
        |  $maybeObservation
        |}""".stripMargin
  }

  def ctrlSubPestJson(ok: Boolean = true) = {
    val maybeMotivates = {
      if (!ok) {
        s""""observation" : ${obsPestJson}"""
      } else {
        ""
      }
    }
    s"""{
        |  "ok" : $ok,
        |  $maybeMotivates
        |}""".stripMargin
  }

  def obsStringJson(propName: String) = {
    s"""{
        |  "note": "This is an observation $propName note",
        |  "$propName" : "The value for $propName is a String"
        |}""".stripMargin
  }

  def obsFromToJson(propName: String) = {
    s"""{
        |  "note": "This is an observation $propName note",
        |  "range": {
        |    "from" : 12.32,
        |    "to" : 24.12
        |  }
        |}""".stripMargin
  }

  def obsPestJson = {
    s"""{
        |  "note": "This is an observation pest note",
        |  "identification" : "termites",
        |  "lifecycles" : [ {
        |    "stage" : "mature colony",
        |    "quantity" : 100
        |  }, {
        |    "stage" : "new colony",
        |    "quantity" : 4
        |  } ]
        |}""".stripMargin
  }

}
