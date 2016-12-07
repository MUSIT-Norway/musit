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

package models.storage

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed trait StorageType extends EnumEntry {

  /**
   * To ensure that it is _safe_ to refactor the Enum entries, we specify a
   * property that will specify the name of a specific implementation.
   * This value will then be used to override the `entryName` attribute.
   *
   * We are still relying on stringly based typing to disambiguate the different
   * storage types, but we can be a bit more confident the stability of the
   * entryName value.
   */
  protected val storageTypeName: String

  // This is lazily initialised to avoid being assigned the dreaded "null"
  override lazy val entryName: String = storageTypeName
}

/**
 * ¡IMPORTANT! Changing the name of the Enum entries will alter the service API!
 *
 * Do not change without notifying and consolidating with API consumers!
 */
object StorageType extends Enum[StorageType] with PlayJsonEnum[StorageType] {
  val values = findValues

  case object RootType extends StorageType {
    override val storageTypeName: String = "Root"
  }

  case object RootLoanType extends StorageType {
    override val storageTypeName: String = "RootLoan"
  }

  case object StorageUnitType extends StorageType {
    override val storageTypeName: String = "StorageUnit"
  }

  case object RoomType extends StorageType {
    override val storageTypeName: String = "Room"
  }

  case object BuildingType extends StorageType {
    override val storageTypeName: String = "Building"
  }

  case object OrganisationType extends StorageType {
    override val storageTypeName: String = "Organisation"
  }

}
