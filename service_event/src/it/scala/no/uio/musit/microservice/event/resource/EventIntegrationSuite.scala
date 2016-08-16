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
import play.api.libs.json.{JsObject, JsValue, Json}
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




    "post Move" in {

      val json =
        """
  {
   "eventType": "Move",
   "note": "Dette er et viktig notat for move!",
   "links": [{"rel": "actor", "href": "actor/12"}]}"""


      val response = createEvent(json)
      response.status mustBe 201
      val moveObject = validateEvent[Move](response.json) // .validate[Move].get

      val responseGet = getEvent(moveObject.id.get)
      responseGet.status mustBe 200
      println(responseGet.body)

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
   "links": [{"rel": "actor", "href": "actor/12"}]}"""

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
   "links": [{"rel": "actor", "href": "actor/12"}]}"""

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
   "links": [{"rel": "actor", "href": "actor/12"}]}"""

    val response = createEvent(json)
    println(s"Create Control temperature: ${response.body}")
    response.status mustBe 201

    val myControlEvent = validateEvent[ControlTemperature](response.json)
    myControlEvent.ok mustBe false
    val responseGet = getEvent(myControlEvent.id.get)
    responseGet.status mustBe 200
    println(s"Get: ${responseGet.body}")

  }

  "post controlTemperature should fail if missing ok-value" in {
    val json =
      """
  {
   "eventType": "ControlTemperature",
   "links": [{"rel": "actor", "href": "actor/12"}]}"""

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
   "links": [{"rel": "actor", "href": "actor/12"}]}"""

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
   "links": [{"rel": "actor", "href": "actor/12"}]}"""

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
    "links": [{"rel": "actor", "href": "actor/12"}]}"""

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
          "links": [{
            "rel": "actor",
            "href": "actor/12"
          }],
          "subEvents-parts": [{
            "eventType": "observationTemperature",
            "from": -30,
            "to": 25,
            "links": [{
              "rel": "actor",
              "href": "actor/12"
            }]
          }, {
            "eventType": "observationTemperature",
            "from": 20,
            "to": 50,
            "links": [{
              "rel": "actor",
              "href": "actor/12"
            }]},
        {
                    "eventType": "observationRelativeHumidity",
                    "from": 1,
                    "to": 2,
                    "links": [{
                      "rel": "actor",
                      "href": "actor/12"
                    }]
            }, {
                    "eventType": "observationInertAir",
                    "from": 0.1,
                    "to": 0.2,
                    "links": [{
                      "rel": "actor",
                      "href": "actor/12"
                    }]}
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


    val airEvent = myEvent.subObservations(3).asInstanceOf[ObservationInertAir]
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
    "links": [{
    "rel": "actor",
    "href": "actor/12"
  },

        {
            "rel": "storageunit-location",
            "href": "storageunit/$storageUnitId"
          }

  ],
    "subEvents-parts": [{
    "eventType": "controlInertluft",
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
    val response = createEvent(postCompositeControlJson)
    println(s"Create: ${response.body}")
    response.status mustBe 201

    val myEvent = validateEvent[Control](response.json)
    myEvent.relatedSubEvents.length mustBe 1

    val parts = myEvent.subEventsWithRelation(EventRelations.relation_parts)
    assert(parts.isDefined)

    val specificControls = parts.get
    assert(specificControls.length >= 2)
    val okControl = specificControls(0).asInstanceOf[ControlInertluft]
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



  "post and get ObservationLys" in {
    val json =
      """
  {
    "eventType": "observationLys",
    "lysforhold": "merkelige forhold",
    "links": [{"rel": "actor", "href": "actor/12"}]}"""

    val response = createEvent(json)
    response.status mustBe 201
    val myEvent = validateEvent[ObservationLys](response.json)
    myEvent.lysforhold mustBe Some("merkelige forhold")


    val responseGet = getEvent(myEvent.id.get)
    responseGet.status mustBe 200
    val myEventGet = validateEvent[ObservationLys](responseGet.json)
    myEventGet.lysforhold mustBe Some("merkelige forhold")
  }

  "post and get ObservationRenhold" in {
    val json =
      """
  {
    "eventType": "observationRenhold",
    "renhold": "merkelige renhold",
    "links": [{"rel": "actor", "href": "actor/12"}]}"""

    val response = createEvent(json)
    response.status mustBe 201
    val myEvent = validateEvent[ObservationRenhold](response.json)
    myEvent.renhold mustBe Some("merkelige renhold")


    val responseGet = getEvent(myEvent.id.get)
    responseGet.status mustBe 200
    val myEventGet = validateEvent[ObservationRenhold](responseGet.json)
    myEventGet.renhold mustBe Some("merkelige renhold")
  }

  "post and get ObservationGass" in {
    val json =
      """
  {
    "eventType": "observationGass",
    "gass": "merkelig gass",
    "links": [{"rel": "actor", "href": "actor/12"}]}"""

    val response = createEvent(json)
    response.status mustBe 201
    val myEvent = validateEvent[ObservationGass](response.json)
    myEvent.gass mustBe Some("merkelig gass")


    val responseGet = getEvent(myEvent.id.get)
    responseGet.status mustBe 200
    val myEventGet = validateEvent[ObservationGass](responseGet.json)
    myEventGet.gass mustBe Some("merkelig gass")
  }

  "post and get ObservationMugg" in {
    val json =
      """
  {
    "eventType": "observationMugg",
    "mugg": "merkelig mugg",
    "links": [{"rel": "actor", "href": "actor/12"}]}"""

    val response = createEvent(json)
    response.status mustBe 201
    val myEvent = validateEvent[ObservationMugg](response.json)
    myEvent.mugg mustBe Some("merkelig mugg")


    val responseGet = getEvent(myEvent.id.get)
    responseGet.status mustBe 200
    val myEventGet = validateEvent[ObservationMugg](responseGet.json)
    myEventGet.mugg mustBe Some("merkelig mugg")
  }

  "post and get ObservationTyveriSikring" in {
    val json =
      """
  {
    "eventType": "observationTyveriSikring",
    "tyverisikring": "merkelig tyveriSikring",
    "links": [{"rel": "actor", "href": "actor/12"}]}"""

    val response = createEvent(json)
    response.status mustBe 201
    val myEvent = validateEvent[ObservationTyveriSikring](response.json)
    myEvent.tyveriSikring mustBe Some("merkelig tyveriSikring")


    val responseGet = getEvent(myEvent.id.get)
    responseGet.status mustBe 200
    val myEventGet = validateEvent[ObservationTyveriSikring](responseGet.json)
    myEventGet.tyveriSikring mustBe Some("merkelig tyveriSikring")
  }

  "post and get ObservationBrannSikring" in {
    val json =
      """
  {
    "eventType": "observationBrannSikring",
    "brannsikring": "merkelig brannSikring",
    "links": [{"rel": "actor", "href": "actor/12"}]}"""

    val response = createEvent(json)
    response.status mustBe 201
    val myEvent = validateEvent[ObservationBrannSikring](response.json)
    myEvent.brannSikring mustBe Some("merkelig brannSikring")


    val responseGet = getEvent(myEvent.id.get)
    responseGet.status mustBe 200
    val myEventGet = validateEvent[ObservationBrannSikring](responseGet.json)
    myEventGet.brannSikring mustBe Some("merkelig brannSikring")
  }

  "post and get ObservationSkallSikring" in {
    val json =
      """
  {
    "eventType": "observationSkallSikring",
    "skallsikring": "merkelig skallSikring",
    "links": [{"rel": "actor", "href": "actor/12"}]}"""

    val response = createEvent(json)
    response.status mustBe 201
    val myEvent = validateEvent[ObservationSkallSikring](response.json)
    myEvent.skallSikring mustBe Some("merkelig skallSikring")


    val responseGet = getEvent(myEvent.id.get)
    responseGet.status mustBe 200
    val myEventGet = validateEvent[ObservationSkallSikring](responseGet.json)
    myEventGet.skallSikring mustBe Some("merkelig skallSikring")
  }

  "post and get ObservationVannskadeRisiko" in {
    val json =
      """
  {
    "eventType": "observationVannskadeRisiko",
    "vannskaderisiko": "merkelig vannskadeRisiko",
    "links": [{"rel": "actor", "href": "actor/12"}]}"""

    val response = createEvent(json)
    response.status mustBe 201
    val myEvent = validateEvent[ObservationVannskadeRisiko](response.json)
    myEvent.vannskadeRisiko mustBe Some("merkelig vannskadeRisiko")


    val responseGet = getEvent(myEvent.id.get)
    responseGet.status mustBe 200
    val myEventGet = validateEvent[ObservationVannskadeRisiko](responseGet.json)
    myEventGet.vannskadeRisiko mustBe Some("merkelig vannskadeRisiko")
  }

  "post and get ObservationSkadedyr" in {
    val json =
      """
  {
          "eventType": "observationSkadedyr",
          "identifikasjon": "skadedyr i veggene",
          "note": "tekst til observationskadedyr",
          "livssykluser": [{
            "livssyklus": "Adult",
            "antall": 3
          }, {
            "livssyklus": "Puppe",
            "antall": 4
          }, {
            "livssyklus": "Puppeskinn",
            "antall": 5
          }, {
            "livssyklus": "Larve",
            "antall": 6
          }, {
            "livssyklus": "Egg",
            "antall": 7
          }]
        }"""

    val response = createEvent(json)
    response.status mustBe 201
    val myEvent = validateEvent[ObservationSkadedyr](response.json)
    myEvent.identifikasjon mustBe Some("skadedyr i veggene")


    val responseGet = getEvent(myEvent.id.get)
    responseGet.status mustBe 200
    val myEventGet = validateEvent[ObservationSkadedyr](responseGet.json)
    myEventGet.identifikasjon mustBe Some("skadedyr i veggene")

    myEventGet.livssykluser.length mustBe 5
    val livsSyklusFirst = myEventGet.livssykluser(0)
    livsSyklusFirst.livssyklus mustBe Some("Adult")
    livsSyklusFirst.antall mustBe Some(3)

    val livsSyklusLast = myEventGet.livssykluser(4)
    livsSyklusLast.livssyklus mustBe Some("Egg")
    livsSyklusLast.antall mustBe Some(7)
    livsSyklusLast.eventId mustBe None //We don't want these in the json output.

  }

  "post and get ObservationSprit" in {
    val json =
      """
  {
      "eventType": "observationSprit",
      "note": "tekst til observationsprit",
       "tilstand": "Uttørket",
       "volum": 3.2
    }"""

    val response = createEvent(json)
    response.status mustBe 201
    val myEvent = validateEvent[ObservationSprit](response.json)


    val responseGet = getEvent(myEvent.id.get)
    responseGet.status mustBe 200
    val myEventGet = validateEvent[ObservationSprit](responseGet.json)

    myEvent.tilstand mustBe Some("Uttørket")
    myEvent.volum mustBe Some(3.2)

  }

  "post and get ObservationSprit without tilstand and volumn" in {
    val json =
      """
  {
      "eventType": "observationSprit",
      "note": "tekst til observationsprit"
    }"""

    val response = createEvent(json)
    response.status mustBe 201
    val myEvent = validateEvent[ObservationSprit](response.json)


    val responseGet = getEvent(myEvent.id.get)
    responseGet.status mustBe 200
    val myEventGet = validateEvent[ObservationSprit](responseGet.json)

    myEvent.tilstand mustBe None
    myEvent.volum mustBe None

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




  "post controlRelativLuftfuktighet" in {
    val json =
      """ {
    "eventType": "controlRelativLuftfuktighet",
    "note": "tekst",
    "ok": true
  }
      """
    val response = createEvent(json)
    response.status mustBe 201

    val myEvent = validateEvent[ControlRelativLuftfuktighet](response.json)
    myEvent.ok mustBe true
  }

  "post ControlLysforhold" in {
    val json =
      """ {
    "eventType": "controlLysforhold",
    "note": "tekst",
    "ok": false
  }
      """
    val response = createEvent(json)
    response.status mustBe 201

    val myEvent = validateEvent[ControlLysforhold](response.json)
    myEvent.ok mustBe false
  }

  "post ControlRenhold" in {
    val json =
      """ {
    "eventType": "controlRenhold",
    "note": "tekst",
    "ok": true
  }
      """
    val response = createEvent(json)
    response.status mustBe 201

    val myEvent = validateEvent[ControlRenhold](response.json)
    myEvent.ok mustBe true
  }


  "post ControlGass" in {
    val json =
      """ {
    "eventType": "controlGass",
    "note": "tekst",
    "ok": false
  }
      """
    val response = createEvent(json)
    response.status mustBe 201

    val myEvent = validateEvent[ControlGass](response.json)
    myEvent.ok mustBe false
  }


  "post ControlMugg" in {
    val json =
      """ {
    "eventType": "controlMugg",
    "note": "tekst",
    "ok": true
  }
      """
    val response = createEvent(json)
    response.status mustBe 201

    val myEvent = validateEvent[ControlMugg](response.json)
    myEvent.ok mustBe true
  }


  "post ControlSkadedyr" in {
    val json =
      """ {
    "eventType": "controlSkadedyr",
    "note": "tekst",
    "ok": false
  }
      """
    val response = createEvent(json)
    response.status mustBe 201

    val myEvent = validateEvent[ControlSkadedyr](response.json)
    myEvent.ok mustBe false
  }

  "post ControlSprit" in {
    val json =
      """ {
    "eventType": "controlSprit",
    "note": "tekst",
    "ok": true
  }
      """
    val response = createEvent(json)
    response.status mustBe 201

    val myEvent = validateEvent[ControlSprit](response.json)
    myEvent.ok mustBe true
  }

  "check that we fail on missing events parameters" in {
    //We deliberately leave out id to get an error.
    val url=s"http://localhost:$port/v1/events?search=[eventType=control, rel=storageunit-location]"

    val response=WS.url(url).get |> waitFutureValue
    response.status mustBe 400
  }

  "check that getEvents" in {
    val response = createEvent(postCompositeControlJson)
    println(s"Create: ${response.body}")
    response.status mustBe 201

//    val controlEvent =  validateEvent[Control](response.json)


    val url=s"http://localhost:$port/v1/events?search=[eventType=control, rel=storageunit-location, id=$storageUnitId]"

    val response2=WS.url(url).get |> waitFutureValue
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

    val controlEvent =  validateEvent[ControlTemperature](response.json)
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
    "links": [{
    "rel": "actor",
    "href": "actor/12"
  }
  ],
    "subEvents-parts": [{
    "eventType": "controlInertluft",
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



}
