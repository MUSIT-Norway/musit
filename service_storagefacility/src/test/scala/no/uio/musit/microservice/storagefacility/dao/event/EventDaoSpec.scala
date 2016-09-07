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

package no.uio.musit.microservice.storagefacility.dao.event

import no.uio.musit.microservice.storagefacility.domain.Interval
import no.uio.musit.microservice.storagefacility.domain.event.EventType
import no.uio.musit.microservice.storagefacility.domain.event.EventTypeRegistry._
import no.uio.musit.microservice.storagefacility.domain.event.control._
import no.uio.musit.microservice.storagefacility.domain.event.dto.{ BaseEventDto, DtoConverters, ExtendedDto }
import no.uio.musit.microservice.storagefacility.domain.event.envreq.EnvRequirement
import no.uio.musit.microservice.storagefacility.testhelpers.EventGenerators
import no.uio.musit.microservices.common.PlayTestDefaults
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{ OneAppPerSuite, PlaySpec }
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

/**
 * Test specs for the EventDao.
 *
 * TODO: Add a lot more test cases!!!
 */
class EventDaoSpec extends PlaySpec
    with OneAppPerSuite
    with ScalaFutures
    with EventGenerators {

  // We need to build up and configure a FakeApplication to get
  // an EventDao with all of the necessary dependencies injected.
  implicit override lazy val app = new GuiceApplicationBuilder()
    .configure(PlayTestDefaults.inMemoryDatabaseConfig())
    .build()

  // This is where we fetch the EventDao singleton from the fake application.
  val eventDao: EventDao = {
    val instance = Application.instanceCache[EventDao]
    instance(app)
  }

  "The EventDao" when {

    "processing controls with sub-controls and observations" should {

      val ctrl = Control(
        baseEvent = createBase("This is a base note"),
        eventType = EventType.fromEventTypeId(ControlEventType.id),
        parts = Some(Seq(
          createTemperatureControl(),
          createAlcoholControl(),
          createCleaningControl(ok = true),
          createPestControl()
        ))
      )

      "succeed when inserting a Control" in {
        val ctrlAsDto = DtoConverters.CtrlConverters.controlToDto(ctrl)

        val futureRes = eventDao.insertEvent(ctrlAsDto)

        whenReady(futureRes) { eventId =>
          eventId mustBe 1L
        }
      }

      "return the Control associated with the provided Id" in {
        val futureRes = eventDao.getEvent(1L)

        whenReady(futureRes) { res =>
          res.isLeft must not be true

          res.right.get match {
            case base: BaseEventDto =>
              val c = DtoConverters.CtrlConverters.controlFromDto(base)

              c.eventType mustBe ctrl.eventType
              c.baseEvent.note mustBe ctrl.baseEvent.note
              c.parts must not be None

            // TODO: Inspect all the parts

            case _ =>
              fail("Expected dto to be of type BaseEventDto")
          }

        }
      }

      val envReq = EnvRequirement(
        baseEvent = createBase("This is the base note"),
        eventType = EventType.fromEventTypeId(EnvRequirementEventType.id),
        temperature = Some(Interval(20, Some(5))),
        airHumidity = Some(Interval(60, Some(10))),
        hypoxicAir = Some(Interval(0, Some(15))),
        cleaning = Some("keep it clean, dude"),
        light = Some("dim")
      )

      "succeed when inserting an Environment Requirement" in {
        val erDto = DtoConverters.EnvReqConverters.envReqToDto(envReq)

        val futureRes = eventDao.insertEvent(erDto)

        whenReady(futureRes) { eventId =>
          eventId mustBe 9L
        }
      }

      "return the Environment Requirement event with the provided ID" in {
        val futureRes = eventDao.getEvent(9L)

        whenReady(futureRes) { res =>
          res.isLeft must not be true

          res.right.get match {
            case ext: ExtendedDto =>
              val er = DtoConverters.EnvReqConverters.envReqFromDto(ext)

              er.eventType mustBe envReq.eventType
              er.baseEvent.note mustBe envReq.baseEvent.note
              er.light mustBe envReq.light
              er.temperature mustBe envReq.temperature
              er.hypoxicAir mustBe envReq.hypoxicAir
              er.airHumidity mustBe envReq.airHumidity
              er.cleaning mustBe envReq.cleaning

            case _ =>
              fail("Expected dto to be of type ExtendedDto")
          }
        }
      }

    }

  }

}
