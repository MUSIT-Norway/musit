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

import no.uio.musit.microservice.actor.domain.{Actor, Organization, OrganizationAddress, Person}
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


  def allActors() : Future[Seq[Actor]] = db.run(ActorTable.result)
  def allPersons() : Future[Seq[Person]] = db.run(PersonTable.result)
  def allOrganizations() : Future[Seq[Organization]] = db.run(OrganizationTable.result)
  def allAddressesForOrganization(id:Long) : Future[Seq[OrganizationAddress]] = db.run(OrganizationAddressTable.filter(_.organizationId === id).result)

  def insertActor(actor: Actor): Future[Actor] = {
    val insertQuery = (ActorTable returning ActorTable.map(_.id) into ((actor, id) => (actor.copy(id=id, links=Seq(LinkService.self(s"/v1/$id"))))))
    val action = insertQuery += actor

    db.run(action)
  }

  def getActorById(id:Long) :Future[Option[Actor]] ={
    val action = ActorTable.filter( _.id === id).result.headOption
    db.run(action)
  }

  private class ActorTable(tag: Tag) extends Table[Actor](tag, "VIEW_ACTOR") {
    def id = column[Long]("NY_ID", O.PrimaryKey, O.AutoInc)// This is the primary key column
    def actorname = column[String]("ACTORNAME")

    def create = (id: Long , actorname:String) => Actor(id, actorname, Seq(LinkService.self(s"/v1/$id")))
    def destroy(actor:Actor) = Some(actor.id, actor.actorname)

    def * = (id, actorname) <> (create.tupled, destroy)
  }

  private class PersonTable(tag: Tag) extends Table[Person](tag, "PERSON") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)// This is the primary key column
    def fn = column[String]("FN")
    def title = column[String]("TITLE")
    def role = column[String]("ROLE")
    def tel = column[String]("TEL")
    def web = column[String]("WEB")
    def email = column[String]("EMAIL")

    def create = (id:Long, fn:String, title:String, role:String, tel:String, web:String, email:String) => Person(id, fn, title, role, tel, web, email, Seq(LinkService.self(s"/v1/person/$id")))
    def destroy(person:Person) = Some(person.id, person.fn, person.title, person.role, person.tel, person.web, person.email)

    def * = (id, fn, title, role, tel, web, email) <> (create.tupled, destroy)
  }

  private class OrganizationTable(tag: Tag) extends Table[Organization](tag, "ORGANIZATION") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)// This is the primary key column
    def fn = column[String]("FN")
    def nickname = column[String]("NiCNAME")
    def tel = column[String]("TEL")
    def web = column[String]("WEB")
    def latitude = column[Double]("LATITUDE")
    def longitude = column[Double]("LONGITUDE")

    def create = (id:Long, fn:String, nickname:String, tel:String, web:String, latitude:Double, longitude:Double) => Organization(id, fn, nickname, tel, web, latitude, longitude, Seq(LinkService.self(s"/v1/organization/$id"), LinkService.local(-1, "addresses", s"/v1/organization/$id/address")))
    def destroy(org:Organization) = Some(org.id, org.fn, org.nickname, org.tel, org.web, org.latitude, org.longitude)

    def * = (id, fn, nickname, tel, web, latitude, longitude) <> (create.tupled, destroy)
  }

  private class OrganizationAddressTable(tag: Tag) extends Table[OrganizationAddress](tag, "ORGANIZATION_ADDRESS") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)// This is the primary key column
    def organizationId = column[Long]("ORGANIZATION_ID")// This is the primary key column
    def addressType = column[String]("TYPE")
    def streetAddress = column[String]("STREET_ADDRESS")
    def locality = column[String]("LOCALITY")
    def postalCode = column[String]("POSTAL_CODE")
    def countryName = column[String]("COUNTRY_NAME")

    def create = (id:Long, organizationId:Long, addressType:String, streetAddress:String, locality:String, postalCode:String, countryName:String) => OrganizationAddress(id, organizationId, addressType, streetAddress, locality, postalCode, countryName, Seq(LinkService.self(s"/v1/organization/$organizationId/address/$id")))
    def destroy(addr:OrganizationAddress) = Some(addr.id, addr.organizationId, addr.addressType, addr.streetAddress, addr.locality, addr.postalCode, addr.countryName)

    def * = (id, organizationId, addressType, streetAddress, locality, postalCode, countryName) <>(create.tupled, destroy)
  }
}


