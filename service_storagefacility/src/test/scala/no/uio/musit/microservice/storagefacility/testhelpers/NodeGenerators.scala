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

package no.uio.musit.microservice.storagefacility.testhelpers

import no.uio.musit.microservice.storagefacility.domain.storage._

trait NodeGenerators {

  val defaultBuilding = {
    Building(
      id = None,
      name = "FooBarBuilding",
      area = Some(200),
      areaTo = Some(250),
      isPartOf = None,
      height = Some(5),
      heightTo = Some(8),
      groupRead = None,
      groupWrite = None,
      address = Some("FooBar Gate 8, 111 Oslo, Norge")
    )
  }

  val defaultStorageUnit = {
    StorageUnit(
      id = None,
      name = "FooUnit",
      area = Some(20),
      areaTo = Some(20),
      isPartOf = None,
      height = Some(2),
      heightTo = Some(2),
      groupRead = None,
      groupWrite = None
    )
  }

}
