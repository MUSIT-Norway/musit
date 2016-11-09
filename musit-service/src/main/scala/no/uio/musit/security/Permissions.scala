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

object Permissions {

  sealed trait Permission {
    val priority: Int
  }

  /**
   * Permission to use when no permissions are required.
   * Typically used when a service needs to be accessible for users regardless
   * of access to museum or collection.
   */
  object Unspecified extends Permission {
    override val priority: Int = 0
  }

  /**
   * Handy permission to use when a service should be usable for authenticated
   * users that aren't registered as users in the system.
   */
  object Guest extends Permission {
    override val priority: Int = 1
  }

  /**
   * Provides READ permission to a service within the context of the
   * potentially additional constraints.
   */
  object Read extends Permission {
    override val priority: Int = 10
  }

  /**
   * Provides WRITE permission to a service within the context of the
   * potentially additional constraints.
   */
  object Write extends Permission {
    override val priority: Int = 20
  }

  /**
   * Provides ADMIN permission to a service within the context of the
   * potentially additional constraints.
   */
  object Admin extends Permission {
    override val priority: Int = 30
  }

  /**
   * Provides application wide ADMIN privileges for _shared_ data across all
   * museums data.
   */
  object MusitAdmin extends Permission {
    override val priority: Int = 40
  }

  /**
   * Highest level of permission available. Should _only_ be used for services
   * that require system/application admin restrictions.
   */
  object GodMode extends Permission {
    override val priority: Int = Int.MaxValue
  }

}
