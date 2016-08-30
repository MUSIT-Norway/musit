/*
 *   MUSIT is a cooperation between the university museums of Norway.
 *   Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License,
 *   or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License along
 *   with this program; if not, write to the Free Software Foundation, Inc.,
 *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package no.uio.musit.microservice.event.resource

import java.sql.Date

import no.uio.musit.microservice.event.domain._
import no.uio.musit.microservice.event.service._
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.PlayTestDefaults._
import no.uio.musit.microservices.common.extensions.PlayExtensions._
import no.uio.musit.microservices.common.extensions.EitherExtensions._
import no.uio.musit.microservices.common.utils.Misc._
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
import play.api.libs.ws.{WS, WSRequest}

/**
  * Created by jstabel on 6/10/16.
  */

object WSRequestFakeHelper {


  implicit class WSRequestImp2(val wsr: WSRequest) extends AnyVal {
    def withFakeUser = wsr.withBearerToken("fake-token-zab-xy-musitTestUser")
  }
}

class EventIntegrationSuite extends PlaySpec with OneServerPerSuite with ScalaFutures {

  import WSRequestFakeHelper._

  val timeout = PlayTestDefaults.timeout
  override lazy val port: Int = 8080
  implicit override lazy val app = new GuiceApplicationBuilder().configure(PlayTestDefaults.inMemoryDatabaseConfig()).build()


  def createEvent(json: String) = {
    WS.url(s"http://localhost:$port/v1/event").withFakeUser.postJsonString(json) |> waitFutureValue
  }

  def createControlEvent(nodeId: Int, json: String) = {
    WS.url(s"http://localhost:$port/v1/node/$nodeId/control").withFakeUser.postJsonString(json) |> waitFutureValue
  }

  def createObservationEvent(nodeId: Int, json: String) = {
    WS.url(s"http://localhost:$port/v1/node/$nodeId/observation").withFakeUser.postJsonString(json) |> waitFutureValue
  }

  def getControlsForNode(nodeId: Int) = {
    WS.url(s"http://localhost:$port/v1/node/$nodeId/controls").get |> waitFutureValue
  }

  def getObservationsForNode(nodeId: Int) = {
    WS.url(s"http://localhost:$port/v1/node/$nodeId/observations").get |> waitFutureValue
  }

  def getEvent(id: Long) = {
    WS.url(s"http://localhost:$port/v1/event/$id").get |> waitFutureValue
  }

  def validateEvent[T <: Event](jsObject: JsValue) = {
    JsonEventHelpers.eventFromJson[T](jsObject).getOrFail
  }

  /*
  def getAndValidateEvent[T <: Event](id: Long) = {
    val resp = getEvent(id)
    validateEvent[T](resp)
  }
*/



  "EventIntegrationSuite " must {


    "getObjectUriViaRelation test" in {
      EventRelations.getObjectUriViaRelation(532, "storageunit-location") mustBe Some("storageunit/532")
    }


    "postWithWrongEvent" in {
      val json =
        """
  {
   "eventType": "hurra",
   "note": "Dette er IKKE viktig notat!"}"""

      val response = createEvent(json)
      response.status mustBe 400
    }

    "postWithControlEvent" in {
      val json =
        """
  {
   "eventType": "Control",
   "note": "Dette er et viktig notat for kontroll!",
   "doneBy": 12}"""

      val response = createEvent(json)
      response.status mustBe 201
      println(s"Create: ${response.body}")

      val myControlEvent = validateEvent[Control](response.json)
      myControlEvent.note mustBe Some("Dette er et viktig notat for kontroll!")
      val responseGet = getEvent(myControlEvent.id.get)
      responseGet.status mustBe 200
      println(s"Get: ${responseGet.body}")

    }
  }



  "post controlTemperature with ok = true" in {
    val json =
      """
  {
   "eventType": "ControlTemperature",
   "ok": true,
   "doneBy": 12}"""

    val response = createEvent(json)
    println(s"Create Control temperature: ${response.body}")
    response.status mustBe 201

    val myControlEvent = validateEvent[ControlTemperature](response.json)
    myControlEvent.ok mustBe true
    val responseGet = getEvent(myControlEvent.id.get)
    responseGet.status mustBe 200
    println(s"Get: ${responseGet.body}")

  }


  "post controlTemperature with ok = false" in {
    val json =
      """
  {
   "eventType": "ControlTemperature",
   "ok": false,
   "doneBy": 12}"""

    val response = createEvent(json)
    response.status mustBe 201

    val myControlEvent = validateEvent[ControlTemperature](response.json)
    myControlEvent.ok mustBe false
    val responseGet = getEvent(myControlEvent.id.get)
    responseGet.status mustBe 200
  }

  "post controlTemperature should fail if missing ok-value" in {
    val json =
      """
  {
   "eventType": "ControlTemperature",
   "doneBy": 12}"""

    val response = createEvent(json)
    println(s"Create Control temperature without ok should fail: ${response.body}")
    response.status mustBe 400
  }



  "post and get envRequirement" in {
    val json =
      """
  {
   "eventType": "EnvRequirement",
   "note": "Dette er et viktig notat for miljøkravene!",
   "temperature": 20,
   "temperatureInterval" : 5,
   "airHumidity": -20,
   "airHumidityInterval" : 4,
   "doneBy": 12}"""

    val response = createEvent(json)
    println(s"Create: ${response.body}")
    response.status mustBe 201

    val myEnvReqEvent = validateEvent[EnvRequirement](response.json)
    myEnvReqEvent.temperature mustBe Some(20)


    val responseGet = getEvent(myEnvReqEvent.id.get)
    responseGet.status mustBe 200
    println(s"Get: ${responseGet.body}")
  }

  "post and get Air envRequirement" in {
    val json =
      """
  {
   "eventType": "EnvRequirement",
   "note": "Dette er et viktig notat for miljøkravene!",
   "airHumidity": -20,
   "airHumidityInterval" : 5,
   "cleaning":"Ikke særlig rent",
   "doneBy": 12}"""

    val response = createEvent(json)
    println(s"Create: ${response.body}")
    response.status mustBe 201
    val myEnvReqEvent = validateEvent[EnvRequirement](response.json) //#OLD Event.format.reads(response.json).get.asInstanceOf[EnvRequirement]
    myEnvReqEvent.airHumidity mustBe Some(-20)
    myEnvReqEvent.envReqDto.cleaning mustBe Some("Ikke særlig rent")


    val responseGet = getEvent(myEnvReqEvent.id.get)
    responseGet.status mustBe 200
    println(s"Get: ${responseGet.body}")
  }



  "post and get ObservationTemperature" in {
    val json =
      """
  {
    "eventType": "observationTemperature",
    "from": -20,
    "to" : 5,
    "doneBy": 12}"""

    val response = createEvent(json)
    println(s"Create: ${response.body}")
    response.status mustBe 201
    val myEvent = validateEvent[ObservationTemperature](response.json)
    myEvent.from mustBe Some(-20)
    myEvent.to mustBe Some(5)


    val responseGet = getEvent(myEvent.id.get)
    responseGet.status mustBe 200
    println(s"Get: ${responseGet.body}")
  }


  "post and get complex Observation" in {
    val json =
      """
        {
          "eventType": "observation",
          "note": "tekst til observasjonene",
          "doneBy": 12,
          "subEvents-parts": [{
            "eventType": "observationTemperature",
            "from": -30,
            "to": 25
          }, {
            "eventType": "observationTemperature",
            "from": 20,
            "to": 50},
        {
                    "eventType": "observationRelativeHumidity",
                    "from": 1,
                    "to": 2
            }, {
                    "eventType": "ObservationHypoxicAir",
                    "from": 0.1,
                    "to": 0.2
            }
          ]
        }"""

    val myRawEvent = validateEvent[Observation](Json.parse(json))
    assert(myRawEvent.subObservations.length >= 2)
    val firstObsTempEvent = myRawEvent.subObservations(0).asInstanceOf[ObservationTemperature]
    firstObsTempEvent.from mustBe Some(-30)
    firstObsTempEvent.to mustBe Some(25)


    val response = createEvent(json)
    println(s"Create: ${response.body}")
    response.status mustBe 201
    val myEvent = validateEvent[Observation](response.json)
    assert(myEvent.subObservations.length >= 3)

    val firstObsEvent = myEvent.subObservations(0).asInstanceOf[ObservationTemperature]
    firstObsEvent.from mustBe Some(-30)
    firstObsEvent.to mustBe Some(25)


    val humEvent = myEvent.subObservations(2).asInstanceOf[ObservationRelativeHumidity]
    humEvent.from mustBe Some(1)
    humEvent.to mustBe Some(2)


    val airEvent = myEvent.subObservations(3).asInstanceOf[ObservationHypoxicAir]
    airEvent.from mustBe Some(0.1)
    airEvent.to mustBe Some(0.2)

    val responseGet = getEvent(myEvent.id.get)
    responseGet.status mustBe 200
    println(s"Get: ${responseGet.body}")
  }



  "post wrong subEvents-relation should result in 400" in {
    val json =
      """
        {
          "eventType": "observation",
          "note": "tekst til observasjonene",

          "subEvents-this_relation_does_not_exist": [{
            "eventType": "observationTemperature",
            "from": -30,
            "to": 25
          }, {
            "eventType": "observationTemperature",
            "from": 20,
            "to": 50
          }]
        }"""
    val response = createEvent(json)
    println(s"Create: ${response.body}")
    response.status mustBe 400
    assert(response.body.contains("this_relation_does_not_exist"))
  }

  val storageUnitId = 532 //arbitrary id for some storageUnit

  val postCompositeControlJson =
    s""" {
    "eventType": "Control",
    "note": "tekst",
    "doneBy": 12,
    "subEvents-parts": [{
    "eventType": "ControlHypoxicAir",
    "ok": true
  }, {
    "eventType": "controlTemperature",
    "ok": false,
    "subEvents-motivates": [{
      "eventType": "observationTemperature",
      "from": 20,
      "to": 50
    }]
  }]
  }
  """


  "post composite control" in {
    val response = createControlEvent(storageUnitId, postCompositeControlJson)
    println(s"Create: ${response.body}")
    response.status mustBe 201

    val myEvent = validateEvent[Control](response.json)
    myEvent.relatedSubEvents.length mustBe 1

    val parts = myEvent.subEventsWithRelation(EventRelations.relation_parts)
    assert(parts.isDefined)

    val specificControls = parts.get
    assert(specificControls.length >= 2)
    val okControl = specificControls(0).asInstanceOf[ControlHypoxicAir]
    val notOkControl = specificControls(1).asInstanceOf[ControlTemperature]

    okControl.ok mustBe true
    notOkControl.ok mustBe false

    val motivatedObservations = notOkControl.subEventsWithRelation(EventRelations.relation_motivates)
    val partsObservations = notOkControl.subEventsWithRelation(EventRelations.relation_parts)

    assert(motivatedObservations.isDefined)
    assert(!partsObservations.isDefined)

    val ObsEvent = motivatedObservations.get(0).asInstanceOf[ObservationTemperature]
    ObsEvent.from mustBe Some(20)
    ObsEvent.to mustBe Some(50)
  }


  "post and get ObservationLightingCondition" in {
    val json =
      """
  {
    "eventType": "ObservationLightingCondition",
    "lightingCondition": "merkelige forhold",
    "doneBy": 12}"""

    val response = createEvent(json)
    response.status mustBe 201
    val myEvent = validateEvent[ObservationLightingCondition](response.json)
    myEvent.lightingCondition mustBe Some("merkelige forhold")


    val responseGet = getEvent(myEvent.id.get)
    responseGet.status mustBe 200
    val myEventGet = validateEvent[ObservationLightingCondition](responseGet.json)
    myEventGet.lightingCondition mustBe Some("merkelige forhold")
  }

  "post and get ObservationCleaning" in {
    val json =
      """
  {
    "eventType": "ObservationCleaning",
    "cleaning": "merkelige renhold",
    "doneBy": 12}"""

    val response = createEvent(json)
    response.status mustBe 201
    val myEvent = validateEvent[ObservationCleaning](response.json)
    myEvent.cleaning mustBe Some("merkelige renhold")


    val responseGet = getEvent(myEvent.id.get)
    responseGet.status mustBe 200
    val myEventGet = validateEvent[ObservationCleaning](responseGet.json)
    myEventGet.cleaning mustBe Some("merkelige renhold")
  }

  "post and get ObservationGas" in {
    val json =
      """
  {
    "eventType": "observationGas",
    "gas": "merkelig gass",
    "doneBy": 12}"""

    val response = createEvent(json)
    response.status mustBe 201
    val myEvent = validateEvent[ObservationGas](response.json)
    myEvent.gas mustBe Some("merkelig gass")


    val responseGet = getEvent(myEvent.id.get)
    responseGet.status mustBe 200
    val myEventGet = validateEvent[ObservationGas](responseGet.json)
    myEventGet.gas mustBe Some("merkelig gass")
  }

  "post and get ObservationMold" in {
    val json =
      """
  {
    "eventType": "ObservationMold",
    "mold": "merkelig mugg",
    "doneBy": 12}"""

    val response = createEvent(json)
    response.status mustBe 201
    val myEvent = validateEvent[ObservationMold](response.json)
    myEvent.mold mustBe Some("merkelig mugg")


    val responseGet = getEvent(myEvent.id.get)
    responseGet.status mustBe 200
    val myEventGet = validateEvent[ObservationMold](responseGet.json)
    myEventGet.mold mustBe Some("merkelig mugg")
  }

  "post and get ObservationTyveriSikring" in {
    val json =
      """
  {
    "eventType": "ObservationTheftProtection",
    "theftProtection": "merkelig tyveriSikring",
    "doneBy": 12}"""

    val response = createEvent(json)
    response.status mustBe 201
    val myEvent = validateEvent[ObservationTheftProtection](response.json)
    myEvent.theftProtection mustBe Some("merkelig tyveriSikring")


    val responseGet = getEvent(myEvent.id.get)
    responseGet.status mustBe 200
    val myEventGet = validateEvent[ObservationTheftProtection](responseGet.json)
    myEventGet.theftProtection mustBe Some("merkelig tyveriSikring")
  }

  "post and get ObservationFireProtection" in {
    val json =
      """
  {
    "eventType": "ObservationFireProtection",
    "fireProtection": "merkelig brannSikring",
    "doneBy": 12}"""

    val response = createEvent(json)
    response.status mustBe 201
    val myEvent = validateEvent[ObservationFireProtection](response.json)
    myEvent.fireProtection mustBe Some("merkelig brannSikring")


    val responseGet = getEvent(myEvent.id.get)
    responseGet.status mustBe 200
    val myEventGet = validateEvent[ObservationFireProtection](responseGet.json)
    myEventGet.fireProtection mustBe Some("merkelig brannSikring")
  }

  "post and get ObservationPerimeterSecurity" in {
    val json =
      """
  {
    "eventType": "ObservationPerimeterSecurity",
    "perimeterSecurity": "merkelig skallSikring",
    "doneBy": 12}"""

    val response = createEvent(json)
    response.status mustBe 201
    val myEvent = validateEvent[ObservationPerimeterSecurity](response.json)
    myEvent.perimeterSecurity mustBe Some("merkelig skallSikring")


    val responseGet = getEvent(myEvent.id.get)
    responseGet.status mustBe 200
    val myEventGet = validateEvent[ObservationPerimeterSecurity](responseGet.json)
    myEventGet.perimeterSecurity mustBe Some("merkelig skallSikring")
  }

  "post and get ObservationWaterDamageAssessment" in {
    val json =
      """
  {
    "eventType": "ObservationWaterDamageAssessment",
    "waterDamageAssessment": "merkelig vannskadeRisiko",
    "doneBy": 12}"""

    val response = createEvent(json)
    response.status mustBe 201
    val myEvent = validateEvent[ObservationWaterDamageAssessment](response.json)
    myEvent.waterDamageAssessment mustBe Some("merkelig vannskadeRisiko")


    val responseGet = getEvent(myEvent.id.get)
    responseGet.status mustBe 200
    val myEventGet = validateEvent[ObservationWaterDamageAssessment](responseGet.json)
    myEventGet.waterDamageAssessment mustBe Some("merkelig vannskadeRisiko")
  }

  "post and get ObservationPest" in {
    val json =
      """
  {
          "eventType": "ObservationPest",
          "identification": "skadedyr i veggene",
          "note": "tekst til observationskadedyr",
          "lifeCycles": [{
            "stage": "Adult",
            "number": 3
          }, {
            "stage": "Puppe",
            "number": 4
          }, {
            "stage": "Puppeskinn",
            "number": 5
          }, {
            "stage": "Larve",
            "number": 6
          }, {
            "stage": "Egg",
            "number": 7
          }]
        }"""

    val response = createEvent(json)
    response.status mustBe 201
    val myEvent = validateEvent[ObservationPest](response.json)
    myEvent.identification mustBe Some("skadedyr i veggene")


    val responseGet = getEvent(myEvent.id.get)
    responseGet.status mustBe 200
    val myEventGet = validateEvent[ObservationPest](responseGet.json)
    myEventGet.identification mustBe Some("skadedyr i veggene")

    myEventGet.livssykluser.length mustBe 5
    val livsSyklusFirst = myEventGet.livssykluser(0)
    livsSyklusFirst.stage mustBe Some("Adult")
    livsSyklusFirst.number mustBe Some(3)

    val livsSyklusLast = myEventGet.livssykluser(4)
    livsSyklusLast.stage mustBe Some("Egg")
    livsSyklusLast.number mustBe Some(7)
    livsSyklusLast.eventId mustBe None //We don't want these in the json output.

  }

  "post and get ObservationAlcohol" in {
    val json =
      """
  {
      "eventType": "ObservationAlcohol",
      "note": "tekst til observationsprit",
       "condition": "Uttørket",
       "volume": 3.2
    }"""

    val response = createEvent(json)
    response.status mustBe 201
    val myEvent = validateEvent[ObservationAlcohol](response.json)


    val responseGet = getEvent(myEvent.id.get)
    responseGet.status mustBe 200
    val myEventGet = validateEvent[ObservationAlcohol](responseGet.json)

    myEvent.condition mustBe Some("Uttørket")
    myEvent.volume mustBe Some(3.2)

  }

  "post and get ObservationAlcohol without tilstand and volumn" in {
    val json =
      """
  {
      "eventType": "ObservationAlcohol",
      "note": "tekst til ObservationAlcohol"
    }"""

    val response = createEvent(json)
    response.status mustBe 201
    val myEvent = validateEvent[ObservationAlcohol](response.json)


    val responseGet = getEvent(myEvent.id.get)
    responseGet.status mustBe 200
    val myEventGet = validateEvent[ObservationAlcohol](responseGet.json)

    myEvent.condition mustBe None
    myEvent.volume mustBe None

  }

  "redefining the same custom field should fail" in {
    intercept[AssertionError] {
      CustomFieldsSpec().defineRequiredBoolean("myBool").defineOptInt("myInt")
    }

    //Bool uses the long field, while string uses the string field, so it should be possible to define both a custom bool and a custom string
    CustomFieldsSpec().defineRequiredBoolean("myBool").defineOptString("myString")

    //And an int and a string
    CustomFieldsSpec().defineRequiredInt("myInt").defineOptString("myString")
  }

  "post ControlRelativeHumidity" in {
    val json =
      """ {
    "eventType": "ControlRelativeHumidity",
    "note": "tekst",
    "ok": true
  }
      """
    val response = createEvent(json)
    response.status mustBe 201

    val myEvent = validateEvent[ControlRelativeHumidity](response.json)
    myEvent.ok mustBe true
  }

  "post ControlLightingCondition" in {
    val json =
      """ {
    "eventType": "ControlLightingCondition",
    "note": "tekst",
    "ok": false
  }
      """
    val response = createEvent(json)
    response.status mustBe 201

    val myEvent = validateEvent[ControlLightingCondition](response.json)
    myEvent.ok mustBe false
  }

  "post ControlCleaning" in {
    val json =
      """ {
    "eventType": "ControlCleaning",
    "note": "tekst",
    "ok": true
  }
      """
    val response = createEvent(json)
    response.status mustBe 201

    val myEvent = validateEvent[ControlCleaning](response.json)
    myEvent.ok mustBe true
  }


  "post ControlGas" in {
    val json =
      """ {
    "eventType": "controlGas",
    "note": "tekst",
    "ok": false
  }
      """
    val response = createEvent(json)
    response.status mustBe 201

    val myEvent = validateEvent[ControlGas](response.json)
    myEvent.ok mustBe false
  }


  "post ControlMold" in {
    val json =
      """ {
    "eventType": "ControlMold",
    "note": "tekst",
    "ok": true
  }
      """
    val response = createEvent(json)
    response.status mustBe 201

    val myEvent = validateEvent[ControlMold](response.json)
    myEvent.ok mustBe true
  }


  "post ControlPest" in {
    val json =
      """ {
    "eventType": "ControlPest",
    "note": "tekst",
    "ok": false
  }
      """
    val response = createEvent(json)
    response.status mustBe 201

    val myEvent = validateEvent[ControlPest](response.json)
    myEvent.ok mustBe false
  }

  "post ControlAlcohol" in {
    val json =
      """ {
    "eventType": "ControlAlcohol",
    "note": "tekst",
    "ok": true
  }
      """
    val response = createEvent(json)
    response.status mustBe 201

    val myEvent = validateEvent[ControlAlcohol](response.json)
    myEvent.ok mustBe true
  }

  "check that we fail on missing events parameters" in {
    //We deliberately leave out id to get an error.
    val url = s"http://localhost:$port/v1/events?search=[eventType=control, rel=storageunit-location]"

    val response = WS.url(url).get |> waitFutureValue
    response.status mustBe 400
  }

  "check that getControls works" in {
    val response = createControlEvent(storageUnitId, postCompositeControlJson)
    response.status mustBe 201

    val controlEvent =  validateEvent[Control](response.json)

    val response2 = getControlsForNode(storageUnitId)
    //#OLD val url=s"http://localhost:$port/v1/events?search=[eventType=control, rel=storageunit-location, id=$storageUnitId]"


    val arrayLength = response2.json match {
      case arr: JsArray => arr.value.length
    }
    assert(arrayLength>=1)


    response2.status mustBe 200
  }



  "check that we can post and get doneDate and doneBy" in {
    val json =
      """ {
    "eventType": "controlTemperature",
    "note": "tekst",
    "ok": true,
    "doneBy": 1,
    "doneDate": "2016-08-01"
  }
      """

    val response = createEvent(json)
    // println(s"Create: ${response.body}")
    response.status mustBe 201

    val controlEvent = validateEvent[ControlTemperature](response.json)
    val myDate: java.sql.Date = new java.sql.Date(DateTime.parse("2016-08-01").getMillis)
    controlEvent.eventDate mustBe Some(myDate)

    controlEvent.relatedActors.length mustBe 1
  }

  "post explicit control on node should fail if wrong eventType" in {
    val json =
      s""" {
    "eventType": "Observation",
    "note": "tekst"
    }
    """

    val response = createControlEvent(1, json)
    println(s"Create: ${response.body}")
    response.status mustBe 400
  }

  def createStorageNode() = 1 //TODO: insert storageNode, but needs to wait for merging of service_event and service_storageAdmin!

  "post explicit control on node" in {
    val json =
      s""" {
    "note": "tekst",
    "subEvents-parts": [{
    "eventType": "ControlHypoxicAir",
    "ok": true
  }, {
    "eventType": "controlTemperature",
    "ok": false,
    "subEvents-motivates": [{
      "eventType": "observationTemperature",
      "from": 20,
      "to": 50
    }]
  }]
  }
  """

    val storageNodeId = createStorageNode()

    val response = createControlEvent(storageNodeId, json)
    println(s"Create: ${response.body}")
    response.status mustBe 201


  }

  "post explicit observation on node" in {
    val json =
      """ {
    "note": "tekst",
    "doneBy": 1,
    "subEvents-parts": [{
    "eventType": "observationTemperature",
    "from": 20,
    "to": 50
  }, {
    "eventType": "ObservationMold",
    "mold":"mye mugg"
  }]
  }
      """

    val storageNodeId = createStorageNode()

    val response = createObservationEvent(storageNodeId, json)
    println(s"Create: ${response.body}")
    response.status mustBe 201


  }
  "get explicit controls on node" in {
    val json =
      s""" {
    "note": "tekst",
    "subEvents-parts": [{
    "eventType": "ControlHypoxicAir",
    "ok": true
  }, {
    "eventType": "controlTemperature",
    "ok": false,
    "subEvents-motivates": [{
      "eventType": "observationTemperature",
      "from": 20,
      "to": 50
    }]
  }]
  }
  """

    val storageNodeId = createStorageNode()

    val response = createControlEvent(storageNodeId, json)
    response.status mustBe 201
    val response2 = createControlEvent(storageNodeId, json)
    //  println(s"Create: ${response.body}")
    response2.status mustBe 201

    val response3 = getControlsForNode(storageNodeId)
    response3.status mustBe 200


  }

  "get explicit observations on node" in {
    val json =
      """ {
    "note": "tekst",
    "doneBy": 1,
    "subEvents-parts": [{
    "eventType": "observationTemperature",
    "from": 20,
    "to": 50
  }, {
    "eventType": "ObservationMold",
    "mold":"mye mugg"
  }]
  }
      """

    val storageNodeId = 777 //createStorageNode()

    val response = createObservationEvent(storageNodeId, json)
    response.status mustBe 201
    val response2 = createObservationEvent(storageNodeId, json)
    val response4 = createObservationEvent(storageNodeId, json)
    val responseOther = createObservationEvent(5252525, json)
    //  println(s"Create: ${response.body}")
    response2.status mustBe 201

    val response3 = getObservationsForNode(storageNodeId)
    response3.status mustBe 200

    val arrayLength = response3.json match {
      case arr: JsArray => arr.value.length
    }
    arrayLength mustBe 3


    val response5 = getObservationsForNode(5252525)
    response5.status mustBe 200

    val arrayLength2 = response5.json match {
      case arr: JsArray => arr.value.length
    }
    arrayLength2 mustBe 1

  }



  "post MoveObject" in {

    val json =
      """
  {
   "eventType": "MoveObject",
   "doneWith": -12345,
   "toPlace" : -666,
   "note": "Dette er et viktig notat for move!"
   }"""

    val response = createEvent(json)
    response.status mustBe 201
    val moveObject = validateEvent[MoveObject](response.json)

    val responseGet = getEvent(moveObject.id.get)
    responseGet.status mustBe 200
    val moveObject2 = validateEvent[MoveObject](responseGet.json)
    moveObject2.relatedPlaces.length mustBe 1
    moveObject2.relatedPlaces.head.placeId mustBe -666
    moveObject2.relatedObjects.length mustBe 1
    moveObject2.relatedObjects.head.objectId mustBe -12345
  }

  "post MovePlace" in {

    val json =
      """
  {
   "eventType": "MovePlace",
   "doneWith": 11,
   "toPlace" : -777,
   "note": "Dette er et viktig notat for move place!"
   }"""


    val response = createEvent(json)
    response.status mustBe 201
    val movePlace = validateEvent[MovePlace](response.json)

    val responseGet = getEvent(movePlace.id.get)
    responseGet.status mustBe 200

    val moveObject2 = validateEvent[MovePlace](responseGet.json)
    moveObject2.relatedPlaces.length mustBe 1
    moveObject2.relatedPlaces.head.placeId mustBe -777

    moveObject2.relatedObjects.length mustBe 1
    moveObject2.relatedObjects.head.objectId mustBe 11
  }

}
