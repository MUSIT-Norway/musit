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

import no.uio.musit.microservice.actor.domain.{Organization, OrganizationAddress, Person}
import no.uio.musit.microservices.common.linking.LinkService
import play.api.Play
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}
import slick.driver.JdbcProfile

import scala.concurrent.Future

object ActorDao extends HasDatabaseConfig[JdbcProfile] {
  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val ActorTable = TableQuery[ActorTable]
  private val PersonTable = TableQuery[PersonTable]
  private val OrganizationTable = TableQuery[OrganizationTable]
  private val OrganizationAddressTable = TableQuery[OrganizationAddressTable]

  /* FINDERS */
  def allPersonsLegacy() : Future[Seq[Person]] = db.run(ActorTable.result)
  def allPersons() : Future[Seq[Person]] = db.run(PersonTable.result)
  def allOrganizations() : Future[Seq[Organization]] = db.run(OrganizationTable.result)
  def allAddressesForOrganization(id:Long) : Future[Seq[OrganizationAddress]] = db.run(OrganizationAddressTable.filter(_.organizationId === id).result)

  def getPersonLegacyById(id:Long) = {
    db.run(ActorTable.filter( _.id === id).result.headOption)
  }

  def getPersonLegacyByName(searchString: String):Future[Seq[Person]] = {
    db.run(ActorTable.filter(_.fn like ("%" + searchString + "%")).result)
  }

  def getPersonById(id:Long) = {
    db.run(PersonTable.filter(_.id === id).result.headOption)
  }

  def getPersonByName(searchString: String):Future[Seq[Person]] = {
    db.run(PersonTable.filter(_.fn like ("%" + searchString + "%")).result)
  }

  def getOrganizationById(id: Long) = {
    db.run(OrganizationTable.filter(_.id === id).result.headOption)
  }

  def getOrganizationByName(searchString: String):Future[Seq[Organization]] = {
    db.run(OrganizationTable.filter(_.fn like ("%" + searchString + "%")).result)
  }

  def getOrganizationAddressById(id: Long) = {
    db.run(OrganizationAddressTable.filter(_.id === id).result.headOption)
  }

  /* CREATES and UPDATES */
  def insertPersonLegacy(actor: Person): Future[Person] = {
    val insertQuery = ActorTable returning ActorTable.map(_.id) into ((actor, id) => actor.copy(id = id, links = Seq(LinkService.self(s"/v1/person/$id"))))
    val action = insertQuery += actor

    db.run(action)
  }

  def insertPerson(person: Person): Future[Person] = {
    val insertQuery = PersonTable returning PersonTable.map(_.id) into ((person, id) => person.copy(id = id, links = Seq(LinkService.self(s"/v1/person/$id"))))
    val action = insertQuery += person

    db.run(action)
  }

  def updatePerson(person: Person): Future[Int] = {
    db.run(PersonTable.filter(_.id === person.id).update(person))
  }

  def insertOrganization(organization: Organization): Future[Organization] = {
    val insertQuery = OrganizationTable returning OrganizationTable.map(_.id) into ((organization, id) => organization.copy(id = id, links = Seq(LinkService.self(s"/v1/organization/$id"), LinkService.local(-1, "addresses", s"/v1/organization/$id/address"))))
    val action = insertQuery += organization

    db.run(action)
  }

  def updateOrganization(organization: Organization) = {
    db.run(OrganizationTable.filter(_.id === organization.id).update(organization))
  }

  def insertOrganizationAddress(address: OrganizationAddress): Future[OrganizationAddress] = {
    val insertQuery = OrganizationAddressTable returning OrganizationAddressTable.map(_.id) into ((addr, id) => addr.copy(id = id, links = Seq(LinkService.self(s"/v1/organization/${address.organizationId}/address/$id"))))
    val action = insertQuery += address

    db.run(action)
  }

  def updateOrganizationAddress(organizationAddress: OrganizationAddress) = {
    db.run(OrganizationAddressTable.filter(_.id === organizationAddress.id).update(organizationAddress))
  }

  /* DELETES */
  def deletePerson(id:Long) = {
    db.run(PersonTable.filter(_.id === id).delete)
  }
  def deleteOrganization(id:Long) = {
    db.run(OrganizationTable.filter(_.id === id).delete)
  }

  def deleteOrganizationAddress(id:Long) = {
    db.run(OrganizationAddressTable.filter(_.id === id).delete)
  }

  /* TABLE DEF using fieldnames from w3c vcard standard */
  private class ActorTable(tag: Tag) extends Table[Person](tag, "VIEW_ACTOR") {
    def id = column[Long]("NY_ID", O.PrimaryKey, O.AutoInc)// This is the primary key column
    def fn = column[String]("ACTORNAME")

    def create = (id: Long , fn:String) => Person(id, fn, links = Seq(LinkService.self(s"/v1/person/$id")))
    def destroy(actor:Person) = Some(actor.id, actor.fn)

    def * = (id, fn) <> (create.tupled, destroy)
  }

  private class PersonTable(tag: Tag) extends Table[Person](tag, "PERSON") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)// This is the primary key column
    def fn = column[String]("FN")
    def title = column[Option[String]]("TITLE")
    def role = column[Option[String]]("ROLE")
    def tel = column[Option[String]]("TEL")
    def web = column[Option[String]]("WEB")
    def email = column[Option[String]]("EMAIL")

    def create = (id:Long, fn:String, title:Option[String], role:Option[String], tel:Option[String], web:Option[String], email:Option[String]) => Person(id, fn, title, role, tel, web, email, Seq(LinkService.self(s"/v1/person/$id")))
    def destroy(person:Person) = Some(person.id, person.fn, person.title, person.role, person.tel, person.web, person.email)

    def * = (id, fn, title, role, tel, web, email) <> (create.tupled, destroy)
  }

  private class OrganizationTable(tag: Tag) extends Table[Organization](tag, "ORGANIZATION") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)// This is the primary key column
    def fn = column[String]("FN")
    def nickname = column[String]("NICKNAME")
    def tel = column[String]("TEL")
    def web = column[String]("WEB")


    def create = (id:Long, fn:String, nickname:String, tel:String, web:String) => Organization(id, fn, nickname, tel, web, Seq(LinkService.self(s"/v1/organization/$id"), LinkService.local(-1, "addresses", s"/v1/organization/$id/address")))
    def destroy(org:Organization) = Some(org.id, org.fn, org.nickname, org.tel, org.web)

    def * = (id, fn, nickname, tel, web) <> (create.tupled, destroy)
  }

  private class OrganizationAddressTable(tag: Tag) extends Table[OrganizationAddress](tag, "ORGANIZATION_ADDRESS") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)// This is the primary key column
    def organizationId = column[Long]("ORGANIZATION_ID")// This is the primary key column
    def addressType = column[String]("TYPE")
    def streetAddress = column[String]("STREET_ADDRESS")
    def locality = column[String]("LOCALITY")
    def postalCode = column[String]("POSTAL_CODE")
    def countryName = column[String]("COUNTRY_NAME")
    def latitude = column[Double]("LATITUDE")
    def longitude = column[Double]("LONGITUDE")

    def create = (id:Long, organizationId:Long, addressType:String, streetAddress:String, locality:String, postalCode:String, countryName:String, latitude:Double, longitude:Double) => OrganizationAddress(id, organizationId, addressType, streetAddress, locality, postalCode, countryName, latitude, longitude, Seq(LinkService.self(s"/v1/organization/$organizationId/address/$id")))
    def destroy(addr:OrganizationAddress) = Some(addr.id, addr.organizationId, addr.addressType, addr.streetAddress, addr.locality, addr.postalCode, addr.countryName, addr.latitude, addr.longitude)

    def * = (id, organizationId, addressType, streetAddress, locality, postalCode, countryName, latitude, longitude) <>(create.tupled, destroy)
  }
}


