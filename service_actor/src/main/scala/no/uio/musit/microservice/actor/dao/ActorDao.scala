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
package no.uio.musit.microservice.actor.dao

import com.google.inject.{ Inject, Singleton }
import no.uio.musit.microservice.actor.domain.{ ActorAuth, Organization, OrganizationAddress, Person }
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.extensions.EitherExtensions._
import no.uio.musit.microservices.common.linking.LinkService
import no.uio.musit.security.SecurityConnection
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.driver.JdbcProfile

import scala.concurrent.Future

@Singleton
class ActorDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  private val ActorTable = TableQuery[ActorTable]
  private val OrganizationTable = TableQuery[OrganizationTable]
  private val OrganizationAddressTable = TableQuery[OrganizationAddressTable]

  /* FINDERS */
  def allPersonsLegacy(): Future[Seq[Person]] = db.run(ActorTable.result)
  def allOrganizations(): Future[Seq[Organization]] = db.run(OrganizationTable.result)
  def allAddressesForOrganization(id: Long): Future[Seq[OrganizationAddress]] = db.run(OrganizationAddressTable.filter(_.organizationId === id).result)

  def getPersonLegacyById(id: Long): Future[Option[Person]] = {
    db.run(ActorTable.filter(_.id === id).result.headOption)
  }

  def getPersonLegacyByName(searchString: String): Future[Seq[Person]] = {
    db.run(ActorTable.filter(_.fn like s"%$searchString%").result)
  }

  def getPersonByDataportenId(dataportenId: String): Future[Option[Person]] = {
    db.run(ActorTable.filter(_.dataportenId === dataportenId).result.headOption)
  }

  def dataportenUserToPerson(securityConnection: SecurityConnection): Person = {
    Person(None, securityConnection.userName, None, None, None, None, securityConnection.userEmail, Some(securityConnection.userId), None)
  }

  def insertActorWithDataportenUserInfo(securityConnection: SecurityConnection): MusitFuture[Person] = {
    val person = dataportenUserToPerson(securityConnection)
    ActorAuth.verifyCanInsertActor(securityConnection, person).toMusitFuture.musitFutureFlatMap { _ =>
      insertPersonLegacy(person).toMusitFuture
    }
  }

  def getOrganizationById(id: Long): Future[Option[Organization]] = {
    db.run(OrganizationTable.filter(_.id === id).result.headOption)
  }

  def getOrganizationByName(searchString: String): Future[Seq[Organization]] = {
    db.run(OrganizationTable.filter(org => (org.fn like s"%$searchString%") || (org.nickname like s"%$searchString%")).result)
  }

  def getOrganizationAddressById(id: Long): Future[Option[OrganizationAddress]] = {
    db.run(OrganizationAddressTable.filter(_.id === id).result.headOption)
  }

  def getPersonDetailsByIds(ids: Set[Long]): Future[Seq[Person]] = {
    db.run(ActorTable.filter(_.id inSet ids).result)
  }

  /* CREATES and UPDATES */
  def insertPersonLegacy(actor: Person): Future[Person] = {
    val insertQuery = ActorTable returning ActorTable.map(_.id) into ((actor, id) => actor.copy(id = id, links = Some(Seq(LinkService.self(s"/v1/person/${id.getOrElse("")}")))))
    val action = insertQuery += actor

    db.run(action)
  }

  def insertOrganization(organization: Organization): Future[Organization] = {
    val insertQuery = OrganizationTable returning OrganizationTable.map(_.id) into ((organization, id) =>
      organization.copy(id = id, links =
        Some(Seq(LinkService.self(s"/v1/organization/${id.getOrElse("")}"), LinkService.local(None, "addresses", s"/v1/organization/${id.getOrElse("")}/address")))))
    val action = insertQuery += organization

    db.run(action)
  }

  def updateOrganization(organization: Organization): Future[Int] = {
    db.run(OrganizationTable.filter(_.id === organization.id).update(organization))
  }

  def insertOrganizationAddress(address: OrganizationAddress): Future[OrganizationAddress] = {
    val insertQuery = OrganizationAddressTable returning OrganizationAddressTable.map(_.id) into ((addr, id) =>
      addr.copy(id = id, links = Some(Seq(LinkService.self(s"/v1/organization/${address.organizationId.getOrElse("")}/address/${id.getOrElse("")}")))))
    val action = insertQuery += address

    db.run(action)
  }

  def updateOrganizationAddress(organizationAddress: OrganizationAddress): Future[Int] = {
    db.run(OrganizationAddressTable.filter(_.id === organizationAddress.id).update(organizationAddress))
  }

  /* DELETES */
  def deleteOrganization(id: Long): Future[Int] = {
    db.run(OrganizationTable.filter(_.id === id).delete)
  }

  def deleteOrganizationAddress(id: Long): Future[Int] = {
    db.run(OrganizationAddressTable.filter(_.id === id).delete)
  }

  /* TABLE DEF using fieldnames from w3c vcard standard */
  private class ActorTable(tag: Tag) extends Table[Person](tag, Some("MUSIT_MAPPING"), "VIEW_ACTOR") {
    val id = column[Option[Long]]("NY_ID", O.PrimaryKey, O.AutoInc) // This is the primary key column
    val fn = column[String]("ACTORNAME")
    val dataportenId = column[Option[String]]("DATAPORTEN_ID")

    val create = (id: Option[Long], fn: String, dataportenId: Option[String]) =>
      Person(
        id,
        fn,
        dataportenId = dataportenId,
        links = Some(Seq(LinkService.self(s"/v1/person/${id.getOrElse("")}")))
      )
    val destroy = (actor: Person) => Some(actor.id, actor.fn, actor.dataportenId)

    def * = (id, fn, dataportenId) <> (create.tupled, destroy)
  }


  private class OrganizationTable(tag: Tag) extends Table[Organization](tag, Some("MUSARK_ACTOR"), "ORGANIZATION") {
    val id = column[Option[Long]]("ID", O.PrimaryKey, O.AutoInc) // This is the primary key column
    val fn = column[String]("FN")
    val nickname = column[String]("NICKNAME")
    val tel = column[String]("TEL")
    val web = column[String]("WEB")

    val create = (id: Option[Long], fn: String, nickname: String, tel: String, web: String) =>
      Organization(id, fn, nickname, tel, web,
        Some(
          Seq(
            LinkService.self(s"/v1/organization/${id.getOrElse("")}"),
            LinkService.local(None, "addresses", s"/v1/organization/${id.getOrElse("")}/address")
          )
        ))
    val destroy = (org: Organization) => Some(org.id, org.fn, org.nickname, org.tel, org.web)

    def * = (id, fn, nickname, tel, web) <> (create.tupled, destroy)
  }

  private class OrganizationAddressTable(tag: Tag) extends Table[OrganizationAddress](tag, Some("MUSARK_ACTOR"), "ORGANIZATION_ADDRESS") {
    val id = column[Option[Long]]("ID", O.PrimaryKey, O.AutoInc) // This is the primary key column
    val organizationId = column[Option[Long]]("ORGANIZATION_ID") // This is the primary key column
    val addressType = column[String]("TYPE")
    val streetAddress = column[String]("STREET_ADDRESS")
    val locality = column[String]("LOCALITY")
    val postalCode = column[String]("POSTAL_CODE")
    val countryName = column[String]("COUNTRY_NAME")
    val latitude = column[Double]("LATITUDE")
    val longitude = column[Double]("LONGITUDE")

    val create = (id: Option[Long], organizationId: Option[Long], addressType: String, streetAddress: String, locality: String,
      postalCode: String, countryName: String, latitude: Double, longitude: Double) =>
      OrganizationAddress(id, organizationId, addressType, streetAddress, locality, postalCode, countryName,
        latitude, longitude, Some(Seq(LinkService.self(s"/v1/organization/$organizationId/address/${id.getOrElse("")}"))))
    val destroy = (addr: OrganizationAddress) =>
      Some(addr.id, addr.organizationId, addr.addressType, addr.streetAddress, addr.locality, addr.postalCode,
        addr.countryName, addr.latitude, addr.longitude)

    def * = (id, organizationId, addressType, streetAddress, locality, postalCode, countryName, latitude, longitude) <> (create.tupled, destroy)
  }
}

