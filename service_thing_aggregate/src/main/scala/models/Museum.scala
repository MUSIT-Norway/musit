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

package models

sealed trait Museum {
  val id: MuseumId
}

object Museum {
  def fromMuseumId(i: MuseumId): Option[Museum] =
    i match {
      case Am.id => Some(Am)
      case Um.id => Some(Um)
      case Khm.id => Some(Khm)
      case Nhm.id => Some(Nhm)
      case Vm.id => Some(Vm)
      case Tmu.id => Some(Tmu)
      case unknown => None
    }
}

case object Am extends Museum {
  val id = MuseumId(1)
}

case object Um extends Museum {
  val id = MuseumId(2)
}

case object Khm extends Museum {
  val id = MuseumId(3)
}

case object Nhm extends Museum {
  val id = MuseumId(4)
}

case object Vm extends Museum {
  val id = MuseumId(5)
}

case object Tmu extends Museum {
  val id = MuseumId(6)
}

