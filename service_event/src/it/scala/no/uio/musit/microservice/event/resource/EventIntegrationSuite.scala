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

import no.uio.musit.microservice.event.domain._
import no.uio.musit.microservice.event.service._
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.PlayTestDefaults._
import no.uio.musit.microservices.common.extensions.PlayExtensions._
import no.uio.musit.microservices.common.extensions.EitherExtensions._
import no.uio.musit.microservices.common.utils.Misc._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.WS

/**
  * Created by jstabel on 6/10/16.
  */


class EventIntegrationSuite extends PlaySpec with OneServerPerSuite with ScalaFutures {
  val timeout = PlayTestDefaults.timeout
  override lazy val port: Int = 8080
  implicit override lazy val app = new GuiceApplicationBuilder().configure(PlayTestDefaults.inMemoryDatabaseConfig()).build()


  def createEvent(json: String) = {
    WS.url(s"http://localhost:$port/v1/event").postJsonString(json) |> waitFutureValue
  }


  def getEvent(id: Long) = {
    WS.url(s"http://localhost:$port/v1/event/$id").get |> waitFutureValue
  }

  def validateEvent[T <: Event](jsObject: JsValue) = {
    EventHelpers.eventFromJson[T](jsObject).getOrFail
  }

  /*
  def getAndValidateEvent[T <: Event](id: Long) = {
    val resp = getEvent(id)
    validateEvent[T](resp)
  }
*/



  "EventIntegrationSuite " must {
    /*"postMove" in {
      val json =
        """
  {
   "eventType": "Move",
   "note": "Dette er et viktig notat!",
   "links": [{"rel": "actor", "href": "actor/12"}]}"""

      val response = createEvent(json)
      response.status mustBe 201
      println(s"Create: ${response.body}")


      val responseGet = getEvent(1)
      responseGet.status mustBe 200
      println(s"Get: ${responseGet.body}")

    }
*/
    /*"post and get observation" in {
      val json =
        """
  {
   "eventType": "Observation",
   "note": "Dette er et viktig notat for observasjon!",
   "temperature": 125,
   "links": [{"rel": "actor", "href": "actor/12"}]}"""

      val response = createEvent(json)
      response.status mustBe 201
      println(s"Create: ${response.body}")

      val myObservationEvent = Event.format.reads(response.json).get.asInstanceOf[Observation]
      myObservationEvent mustBe Some(125)


      val responseGet = getEvent(1)
      responseGet.status mustBe 200
      println(s"Get: ${responseGet.body}")

    }
*/
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
   "links": [{"rel": "actor", "href": "actor/12"}]}"""

    val response = createEvent(json)
    println(s"Create: ${response.body}")
    response.status mustBe 201
    val myEnvReqEvent = validateEvent[EnvRequirement](response.json) //#OLD Event.format.reads(response.json).get.asInstanceOf[EnvRequirement]
    myEnvReqEvent.airHumidity mustBe Some(-20)


    val responseGet = getEvent(myEnvReqEvent.id.get)
    responseGet.status mustBe 200
    println(s"Get: ${responseGet.body}")
  }



  "post and get ObservationTemperature" in {
    val json =
      """
  {
    "eventType": "observationTemperature",
    "temperatureFrom": -20,
    "temperatureTo" : 5,
    "links": [{"rel": "actor", "href": "actor/12"}]}"""

    val response = createEvent(json)
    println(s"Create: ${response.body}")
    response.status mustBe 201
    val myEvent = validateEvent[ObservationTemperature](response.json)
    myEvent.temperatureFrom mustBe Some(-20)
    myEvent.temperatureTo mustBe Some(5)


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
        	"subEvents": [{
        		"eventType": "observationTemperature",
        		"temperatureFrom": -30,
        		"temperatureTo": 25,
        		"links": [{
        			"rel": "actor",
        			"href": "actor/12"
        		}]
        	}]
        }"""

    val myRawEvent = validateEvent[Observation](Json.parse(json))
    val firstObsTempEvent = myRawEvent.subObservations(0).asInstanceOf[ObservationTemperature]
    firstObsTempEvent.temperatureFrom mustBe Some(-30)
    firstObsTempEvent.temperatureTo mustBe Some(25)


    val response = createEvent(json)
    println(s"Create: ${response.body}")
    response.status mustBe 201
    val myEvent = validateEvent[Observation](response.json)
    assert(myEvent.subObservations.length > 0)

    val firstObsEvent = myEvent.subObservations(0).asInstanceOf[ObservationTemperature]
    firstObsEvent.temperatureFrom mustBe Some(-30)
    firstObsEvent.temperatureTo mustBe Some(25)


    val responseGet = getEvent(myEvent.id.get)
    responseGet.status mustBe 200
    println(s"Get: ${responseGet.body}")
  }

}

