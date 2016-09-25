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

package no.uio.musit.microservice.storagefacility.resource

import no.uio.musit.microservice.storagefacility.test.BaseUri
import no.uio.musit.test.MusitSpecWithServerPerSuite
import org.scalatest.time.{Millis, Seconds, Span}

class EventResourceIntegrationSpec extends MusitSpecWithServerPerSuite {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  "The storage facility event service" should {
    "successfully register a new control" in {
      pending
    }

    "get a specific control for a node" in {
      pending
    }

    "successfully register another control" in {
      pending
    }

    "successfully register a new observation" in {
      pending
    }

    "get a specific observation for a node" in {
      pending
    }

    "successfully register another observation" in {
      pending
    }

    "list all controls and observations for a node, ordered by doneDate" in {
      pending
    }

  }

}