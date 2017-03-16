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

package no.uio.musit.security

import play.api.libs.json._

object Permissions {

  sealed trait Permission {
    val priority: Int
  }

  sealed trait ElevatedPermission extends Permission

  object Permission {

    implicit val reads: Reads[Permission] = __.read[Int].map(fromInt)

    implicit val writes: Writes[Permission] = Writes(p => JsNumber(p.priority))

    def fromInt(pri: Int): Permission = {
      pri match {
        case Guest.priority      => Guest
        case Read.priority       => Read
        case Write.priority      => Write
        case Admin.priority      => Admin
        case MusitAdmin.priority => MusitAdmin
        case GodMode.priority    => GodMode
        case _                   => Unspecified
      }
    }

  }

  /**
   * Permission to use when no permissions are required.
   * Typically used when a service needs to be accessible for users regardless
   * of access to museum or collection.
   */
  case object Unspecified extends Permission {
    override val priority: Int = 0
  }

  /**
   * Handy permission to use when a service should be usable for authenticated
   * users that aren't registered as users in the system.
   */
  case object Guest extends Permission {
    override val priority: Int = 1
  }

  /**
   * Provides READ permission to a service within the context of the
   * potentially additional constraints.
   */
  case object Read extends Permission {
    override val priority: Int = 10
  }

  /**
   * Provides WRITE permission to a service within the context of the
   * potentially additional constraints.
   */
  case object Write extends Permission {
    override val priority: Int = 20
  }

  /**
   * Provides ADMIN permission to a service within the context of the
   * potentially additional constraints.
   */
  case object Admin extends Permission {
    override val priority: Int = 30
  }

  /**
   * Provides application wide ADMIN privileges for _shared_ data across all
   * museums data.
   */
  case object MusitAdmin extends ElevatedPermission {
    override val priority: Int = 40
  }

  /**
   * Highest level of permission available. Should _only_ be used for services
   * that require system/application admin restrictions.
   */
  case object GodMode extends ElevatedPermission {
    override val priority: Int = 10000
  }

}
