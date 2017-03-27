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

package repositories.dao

import com.google.inject.{Inject, Singleton}
import models.OrganisationAddress
import no.uio.musit.models.{DatabaseId, OrgId}
import no.uio.musit.MusitResults._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

@Singleton
class AddressDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile]
    with ColumnTypeMappers {

  import profile.api._

  private val orgAdrTable = TableQuery[OrganisationAddressTable]

  def allFor(id: OrgId): Future[Seq[OrganisationAddress]] = {
    db.run(orgAdrTable.filter(_.organisationId === id).result)
  }

  def getById(orgId: OrgId, id: DatabaseId): Future[Option[OrganisationAddress]] = {
    db.run(orgAdrTable.filter { a =>
      a.id === id && a.organisationId === orgId
    }.result.headOption)
  }

  def insert(address: OrganisationAddress): Future[OrganisationAddress] = {
    val insertQuery = orgAdrTable returning
      orgAdrTable.map(_.id) into ((addr, id) => addr.copy(id = id))

    val action = insertQuery += address

    db.run(action)
  }

  def update(orgAddr: OrganisationAddress): Future[MusitResult[Option[Int]]] = {
    val query = orgAdrTable.filter(_.id === orgAddr.id).update(orgAddr)
    db.run(query).map {
      case upd: Int if upd == 0 => MusitSuccess(None)
      case upd: Int if upd == 1 => MusitSuccess(Some(upd))
      case upd: Int if upd > 1  => MusitDbError("Too many records were updated")
    }
  }

  def delete(id: DatabaseId): Future[Int] = {
    db.run(orgAdrTable.filter(_.id === id).delete)
  }

  private class OrganisationAddressTable(
      tag: Tag
  ) extends Table[OrganisationAddress](tag, Some(SchemaName), OrgAdrTableName) {

    val id             = column[Option[DatabaseId]]("ORGADDRESSID", O.PrimaryKey, O.AutoInc)
    val organisationId = column[Option[OrgId]]("ORG_ID")
    val addressType    = column[String]("ADDRESS_TYPE")
    val streetAddress  = column[String]("STREET_ADDRESS")
    val locality       = column[String]("LOCALITY")
    val postalCode     = column[String]("POSTAL_CODE")
    val countryName    = column[String]("COUNTRY_NAME")
    val latitude       = column[Double]("LATITUDE")
    val longitude      = column[Double]("LONGITUDE")

    val create = (
        id: Option[DatabaseId],
        organisationId: Option[OrgId],
        addressType: String,
        streetAddress: String,
        locality: String,
        postalCode: String,
        countryName: String,
        latitude: Double,
        longitude: Double
    ) =>
      OrganisationAddress(
        id = id,
        organisationId = organisationId,
        addressType = addressType,
        streetAddress = streetAddress,
        locality = locality,
        postalCode = postalCode,
        countryName = countryName,
        latitude = latitude,
        longitude = longitude
    )

    val destroy = (addr: OrganisationAddress) =>
      Some(
        (
          addr.id,
          addr.organisationId,
          addr.addressType,
          addr.streetAddress,
          addr.locality,
          addr.postalCode,
          addr.countryName,
          addr.latitude,
          addr.longitude
        )
    )

    // scalastyle:off method.name
    def * =
      (
        id,
        organisationId,
        addressType,
        streetAddress,
        locality,
        postalCode,
        countryName,
        latitude,
        longitude
      ) <> (create.tupled, destroy)

    // scalastyle:on method.name
  }

}
