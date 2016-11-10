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
        case unknown => None
      }
  }

  // TODO: The ID's need to be aligned with the ID's in the DB.
  // TODO: fake_security.json needs to be modified to align with the changed ID's.

  /*
    1   Arkeologisk Museum  AM
    2   Bergen Universitetsmuseum   UM
    3   Kulturhistorisk Museum  KHM
    4   Naturhistorisk Museum   NHM
    5   NTNU Vitenskapsmuseet   VM
    6   Tromsø Museum  TMU
    7   Kristiansand Naturmuseum    KMN
  */

  /**
   * Gives access to all museums...needed to provide cross museum
   * permissions in certain Roles.
   */
  case object All extends Museum {
    val id = MuseumId(Int.MaxValue)
  }

  /**
   * Kulturhistorisk museum i Oslo
   */
  case object Khm extends Museum {
    val id = MuseumId(1)
  }

  /**
   * Arkeologisk museum i Stavanger
   */
  case object Am extends Museum {
    val id = MuseumId(2)
  }

  /**
   * Universitetsmuseet i Bergen
   */
  case object Um extends Museum {
    val id = MuseumId(3)
  }

  /**
   * Naturhistorisk museum i Oslo
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
