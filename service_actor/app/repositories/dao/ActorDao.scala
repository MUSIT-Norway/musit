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
import models.{Organization, OrganizationAddress, Person}
import models.{OrganizationAddress, Person}
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.service.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

@Singleton
class ActorDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  // FIXME: Split into 3 different Dao impls

  private val actorTable = TableQuery[ActorTable]
  private val orgTable = TableQuery[OrganizationTable]
  private val orgAddrTable = TableQuery[OrganizationAddressTable]

  def allAddressesForOrganization(id: Long): Future[Seq[OrganizationAddress]] = {
    db.run(orgAddrTable.filter(_.organizationId === id).result)
  }

  def getPersonLegacyById(id: Long): Future[Option[Person]] = {
    db.run(actorTable.filter(_.id === id).result.headOption)
  }

  def getPersonLegacyByName(searchString: String): Future[Seq[Person]] = {
    val likeArg = searchString.toUpperCase
    db.run(actorTable.filter(_.fn.toUpperCase like s"%$likeArg%").result)
  }

  def getPersonByDataportenId(dataportenId: String): Future[Option[Person]] = {
    db.run(actorTable.filter(_.dataportenId === dataportenId).result.headOption)
  }

  // TODO: Move to Person companion
  def dataportenUserToPerson(user: AuthenticatedUser): Person = {
    Person(
      id = None,
      fn = user.userInfo.name.getOrElse(""),
      title = None,
      role = None,
      tel = None,
      web = None,
      email = user.userInfo.email,
      dataportenId = Some(user.userInfo.id)
    )
  }

  def insertAuthenticatedUser(user: AuthenticatedUser): Future[Person] = {
    val person = dataportenUserToPerson(user)
    insertPersonLegacy(person)
  }

  def getOrganizationById(id: Long): Future[Option[Organization]] = {
    db.run(orgTable.filter(_.id === id).result.headOption)
  }

  def getOrganizationByName(searchString: String): Future[Seq[Organization]] = {
    val query = orgTable.filter { org =>
      (org.fn like s"%$searchString%") || (org.nickname like s"%$searchString%")
    }
    db.run(query.result)
  }

  def getOrganizationAddressById(id: Long): Future[Option[OrganizationAddress]] = {
    db.run(orgAddrTable.filter(_.id === id).result.headOption)
  }

  def getPersonDetailsByIds(ids: Set[Long]): Future[Seq[Person]] = {
    db.run(actorTable.filter(_.id inSet ids).result)
  }

  /* CREATES and UPDATES */
  def insertPersonLegacy(actor: Person): Future[Person] = {
    val insQuery = actorTable returning
      actorTable.map(_.id) into ((actor, id) => actor.copy(id = id))

    val action = insQuery += actor

    db.run(action)
  }

  def insertOrganization(organization: Organization): Future[Organization] = {
    val insertQuery = orgTable returning
      orgTable.map(_.id) into ((organization, id) => organization.copy(id = id))

    val action = insertQuery += organization

    db.run(action)
  }

  def updateOrganization(org: Organization): Future[MusitResult[Option[Int]]] = {
    // "Record was updated!"
    val query = orgTable.filter(_.id === org.id).update(org)
    db.run(query).map {
      case numUpd: Int if numUpd == 0 => MusitSuccess(None)
      case numUpd: Int if numUpd == 1 => MusitSuccess(Some(numUpd))
      case numUpd: Int if numUpd > 1 => MusitDbError("Too many records were updated")
    }
  }

  def insertOrganizationAddress(address: OrganizationAddress): Future[OrganizationAddress] = {
    val insertQuery = orgAddrTable returning
      orgAddrTable.map(_.id) into ((addr, id) => addr.copy(id = id))

    val action = insertQuery += address

    db.run(action)
  }

  def updateOrganizationAddress(orgAddr: OrganizationAddress): Future[MusitResult[Option[Int]]] = {
    val query = orgAddrTable.filter(_.id === orgAddr.id).update(orgAddr)
    db.run(query).map {
      case numUpd: Int if numUpd == 0 => MusitSuccess(None)
      case numUpd: Int if numUpd == 1 => MusitSuccess(Some(numUpd))
      case numUpd: Int if numUpd > 1 => MusitDbError("Too many records were updated")
    }
  }

  /* DELETES */
  def deleteOrganization(id: Long): Future[Int] = {
    db.run(orgTable.filter(_.id === id).delete)
  }

  def deleteOrganizationAddress(id: Long): Future[Int] = {
    db.run(orgAddrTable.filter(_.id === id).delete)
  }

  /* TABLE DEF using fieldnames from w3c vcard standard */
  private class ActorTable(
      tag: Tag
  ) extends Table[Person](tag, Some("MUSIT_MAPPING"), "VIEW_ACTOR") {

    val id = column[Option[Long]]("NY_ID", O.PrimaryKey, O.AutoInc)
    val fn = column[String]("ACTORNAME")
    val dataportenId = column[Option[String]]("DATAPORTEN_ID")

    val create = (id: Option[Long], fn: String, dataportenId: Option[String]) =>
      Person(
        id = id,
        fn = fn,
        dataportenId = dataportenId
      )

    val destroy = (actor: Person) => Some((actor.id, actor.fn, actor.dataportenId))

    // scalastyle:off method.name
    def * = (id, fn, dataportenId) <> (create.tupled, destroy)

    // scalastyle:on method.name
  }

  private class OrganizationTable(
      tag: Tag
  ) extends Table[Organization](tag, Some("MUSARK_ACTOR"), "ORGANIZATION") {

    val id = column[Option[Long]]("ID", O.PrimaryKey, O.AutoInc)
    val fn = column[String]("FN")
    val nickname = column[String]("NICKNAME")
    val tel = column[String]("TEL")
    val web = column[String]("WEB")

    val create = (
      id: Option[Long],
      fn: String,
      nickname: String,
      tel: String,
      web: String
    ) => Organization(id, fn, nickname, tel, web)

    val destroy = (org: Organization) =>
      Some((org.id, org.fn, org.nickname, org.tel, org.web))

    // scalastyle:off method.name
    def * = (id, fn, nickname, tel, web) <> (create.tupled, destroy)

    // scalastyle:on method.name
  }

  private class OrganizationAddressTable(
      tag: Tag
  ) extends Table[OrganizationAddress](tag, Some("MUSARK_ACTOR"), "ORGANIZATION_ADDRESS") {

    val id = column[Option[Long]]("ID", O.PrimaryKey, O.AutoInc)
    val organizationId = column[Option[Long]]("ORGANIZATION_ID")
    val addressType = column[String]("TYPE")
    val streetAddress = column[String]("STREET_ADDRESS")
    val locality = column[String]("LOCALITY")
    val postalCode = column[String]("POSTAL_CODE")
    val countryName = column[String]("COUNTRY_NAME")
    val latitude = column[Double]("LATITUDE")
    val longitude = column[Double]("LONGITUDE")

    val create = (
      id: Option[Long],
      organizationId: Option[Long],
      addressType: String,
      streetAddress: String,
      locality: String,
      postalCode: String,
      countryName: String,
      latitude: Double,
      longitude: Double
    ) => OrganizationAddress(
      id = id,
      organizationId = organizationId,
      addressType = addressType,
      streetAddress = streetAddress,
      locality = locality,
      postalCode = postalCode,
      countryName = countryName,
      latitude = latitude,
      longitude = longitude
    )

    val destroy = (addr: OrganizationAddress) =>
      Some((
        addr.id,
        addr.organizationId,
        addr.addressType,
        addr.streetAddress,
        addr.locality,
        addr.postalCode,
        addr.countryName,
        addr.latitude,
        addr.longitude
      ))

    // scalastyle:off method.name
    def * = (
      id,
      organizationId,
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

