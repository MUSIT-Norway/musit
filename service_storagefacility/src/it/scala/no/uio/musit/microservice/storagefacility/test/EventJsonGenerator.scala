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

  def baseEventJson(affectedThingId: Int, eventType: String, regDay: Int) = {
    s""""doneDate" : "2016-09-${regDay}T09:17:46+02:00",
        |"doneBy" : {
        |  "roleId" : 1,
        |  "actorId" : 12
        |},
        |"note" : "This is a $eventType note",
        |"registeredBy" : "Darth Vader",
        |"registeredDate" : "2016-09-${regDay}T09:17:46+02:00",
        |"eventType" : "$eventType",
        |"affectedThing" : {
        |  "roleId" : 1,
        |  "objectId" : $affectedThingId
        |}""".stripMargin
  }

  def controlJson(affectedThingId: Int, regDay: Int) = {
    s"""{
        |  ${baseEventJson(affectedThingId, "Control", regDay)},
        |  "parts" : [
        |    ${ctrlSubFromToJson(affectedThingId, "ControlTemperature", regDay, ok = false)},
        |    ${ctrlSubFromToJson(affectedThingId, "ControlAlcohol", regDay, ok = false)},
        |    ${ctrlSubStringJson(affectedThingId, "ControlCleaning", regDay, ok = false)},
        |    ${ctrlSubPestJson(affectedThingId, regDay, ok = false)}
        |  ]
        |}""".stripMargin
  }

  def motivates(affectedThingId: Int, etype: String, regDay: Int, ok: Boolean)(obsJson: (Int, String, Int) => String) = {
    if (!ok) {
      s""""motivates" : ${obsJson(affectedThingId, etype.replaceAll("Control", "Observation"), regDay)}"""
    } else {
      ""
    }
  }

  def ctrlSubStringJson(affectedThingId: Int, etype: String, regDay: Int, ok: Boolean = true) = {
    val maybeMotivates = motivates(affectedThingId, etype, regDay, ok)(obsStringJson)
    s"""{
        |  ${baseEventJson(affectedThingId, etype, regDay)},
        |  "ok" : $ok,
        |  $maybeMotivates
        |}""".stripMargin
  }

  def ctrlSubFromToJson(affectedThingId: Int, etype: String, regDay: Int, ok: Boolean = true) = {
    val maybeMotivates = motivates(affectedThingId, etype, regDay, ok)(obsFromToJson)
    s"""{
        |  ${baseEventJson(affectedThingId, etype, regDay)},
        |  "ok" : $ok,
        |  $maybeMotivates
        |}""".stripMargin
  }

  def ctrlSubPestJson(affectedThingId: Int, regDay: Int, ok: Boolean = true) = {
    val maybeMotivates = {
      if (!ok) {
        s""""motivates" : ${obsPestJson(affectedThingId, regDay)}"""
      } else {
        ""
      }
    }
    s"""{
        |  ${baseEventJson(affectedThingId, "ControlPest", regDay)},
        |  "ok" : $ok,
        |  $maybeMotivates
        |}""".stripMargin
  }

  def obsStringJson(affectedThingId: Int, etype: String, regDay: Int) = {
    val propName: String = etype.toLowerCase.stripPrefix("observation")
    s"""{
        |  ${baseEventJson(affectedThingId, etype, regDay)},
        |  "$propName" : "The value for $propName is a String"
        |}""".stripMargin
  }

  def obsFromToJson(affectedThingId: Int, etype: String, regDay: Int) = {
    s"""{
        |  ${baseEventJson(affectedThingId, etype, regDay)},
        |  "from" : 12.32,
        |  "to" : 24.12
        |}""".stripMargin
  }

  def obsPestJson(affectedThingId: Int, regDay: Int) = {
    s"""{
        |  ${baseEventJson(affectedThingId, "ObservationPest", regDay)},
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
