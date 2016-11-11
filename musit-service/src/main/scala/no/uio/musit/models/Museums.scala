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

package no.uio.musit.models

object Museums {

  sealed trait Museum {
    val id: MuseumId
  }

  object Museum {
    def fromMuseumId(i: MuseumId): Option[Museum] =
      i match {
        case Khm.id => Some(Khm)
        case Am.id => Some(Am)
        case Um.id => Some(Um)
        case Nhm.id => Some(Nhm)
        case Vm.id => Some(Vm)
        case Tmu.id => Some(Tmu)
        case Kmn.id => Some(Kmn)
        case Test.id => Some(Test)
        case unknown => None
      }
  }

  /**
   * Gives access to all museums...needed to provide cross museum
   * permissions in certain Roles.
   */
  case object All extends Museum {
    val id = MuseumId(Int.MaxValue)
  }

  /**
   * Museum
   */
  case object Test extends Museum {
    val id = MuseumId(99)
  }

  /**
   * Museum of Archeology in Stavanger
   */
  case object Am extends Museum {
    val id = MuseumId(1)
  }

  /**
   * The university museum in Bergen
   */
  case object Um extends Museum {
    val id = MuseumId(2)
  }

  /**
   * Cultural history museum in Oslo
   */
  case object Khm extends Museum {
    val id = MuseumId(3)
  }

  /**
   * Natural history museum in Oslo
   */
  case object Nhm extends Museum {
    val id = MuseumId(4)
  }

  /**
   * Vitenskapsmuseet i Trondheim (NTNU)
   */
  case object Vm extends Museum {
    val id = MuseumId(5)
  }

  /**
   * Tromsø museum
   */
  case object Tmu extends Museum {
    val id = MuseumId(6)
  }

  /**
   * Kristiansand naturmuseum
   */
  case object Kmn extends Museum {
    val id = MuseumId(7)
  }

}
