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

import play.api.libs.json.{JsObject, Json}

object Museums {

  sealed trait Museum { self =>
    val id: MuseumId
    val shortName: String
  }

  val museums = List(All, Test, Am, Um, Khm, Nhm, Vm, Tmu, Kmn)

  object Museum {

    def museumIdToString(i: MuseumId) =
      fromMuseumId(i).map(_.shortName).getOrElse("")

    def fromMuseumId(i: MuseumId): Option[Museum] =
      i match {
        case Test.id => Some(Test)
        case Khm.id  => Some(Khm)
        case Am.id   => Some(Am)
        case Um.id   => Some(Um)
        case Nhm.id  => Some(Nhm)
        case Vm.id   => Some(Vm)
        case Tmu.id  => Some(Tmu)
        case Kmn.id  => Some(Kmn)
        case unknown => None
      }

    def fromShortName(s: String): Option[Museum] =
      s match {
        case Test.shortName => Some(Test)
        case Khm.shortName  => Some(Khm)
        case Am.shortName   => Some(Am)
        case Um.shortName   => Some(Um)
        case Nhm.shortName  => Some(Nhm)
        case Vm.shortName   => Some(Vm)
        case Tmu.shortName  => Some(Tmu)
        case Kmn.shortName  => Some(Kmn)
        case unknown        => None
      }

    def toJson(m: Museum): JsObject = {
      Json.obj(
        "id"        -> m.id.underlying,
        "shortName" -> m.shortName
      )
    }
  }

  /**
   * Gives access to all museums...needed to provide cross museum
   * permissions in certain Roles.
   */
  case object All extends Museum {
    override val id                = MuseumId(10000)
    override val shortName: String = this.productPrefix
  }

  /**
   * Museum
   */
  case object Test extends Museum {
    override val id                = MuseumId(99)
    override val shortName: String = this.productPrefix
  }

  /**
   * Museum of Archeology in Stavanger
   */
  case object Am extends Museum {
    override val id                = MuseumId(1)
    override val shortName: String = this.productPrefix
  }

  /**
   * The university museum in Bergen
   */
  case object Um extends Museum {
    override val id                = MuseumId(2)
    override val shortName: String = this.productPrefix
  }

  /**
   * Cultural history museum in Oslo
   */
  case object Khm extends Museum {
    override val id                = MuseumId(3)
    override val shortName: String = this.productPrefix
  }

  /**
   * Natural history museum in Oslo
   */
  case object Nhm extends Museum {
    override val id                = MuseumId(4)
    override val shortName: String = this.productPrefix
  }

  /**
   * Science museum in Trondheim (NTNU)
   */
  case object Vm extends Museum {
    override val id                = MuseumId(5)
    override val shortName: String = this.productPrefix
  }

  /**
   * Tromsø museum
   */
  case object Tmu extends Museum {
    override val id                = MuseumId(6)
    override val shortName: String = this.productPrefix
  }

  /**
   * Kristiansand Nature museum
   */
  case object Kmn extends Museum {
    override val id                = MuseumId(7)
    override val shortName: String = this.productPrefix
  }

}
