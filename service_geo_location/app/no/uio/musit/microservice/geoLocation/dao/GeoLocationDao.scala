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
package no.uio.musit.microservice.geoLocation.dao

import no.uio.musit.microservice.geoLocation.domain.GeoLocation
import no.uio.musit.microservices.common.linking.LinkService
import play.api.Play
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}
import slick.driver.JdbcProfile

import scala.concurrent.Future

object GeoLocationDao extends HasDatabaseConfig[JdbcProfile] {
  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val GeoLocationTable = TableQuery[GeoLocationTable]


  def all() : Future[Seq[GeoLocation]] = db.run(GeoLocationTable.result)

  def insert(geoLocation: GeoLocation): Future[GeoLocation] = {
    val insertQuery = (GeoLocationTable returning GeoLocationTable.map(_.id) into ((geoLocation, id) => (geoLocation.copy(id=id, links=Seq(LinkService.self(s"/v1/$id"))))))
    val action = insertQuery += geoLocation

    db.run(action)
  }

  def getById(id:Long) :Future[Option[GeoLocation]] ={
    val action = GeoLocationTable.filter( _.id === id).result.headOption
    db.run(action)
  }

  private class GeoLocationTable(tag: Tag) extends Table[GeoLocation](tag,Some("musark_geoLocation"), "ADRESSE") {
    def id = column[Long]("NY_ID", O.PrimaryKey, O.AutoInc)// This is the primary key column
    def address = column[String]("ADDRESS")


    def create = (id: Long , address:String) => GeoLocation(id, address, Seq(LinkService.self(s"/v1/$id")))
    def destroy(geoLocation:GeoLocation) = Some(geoLocation.id, geoLocation.address)

    def * = (id, address) <>(create.tupled, destroy)
  }
}


