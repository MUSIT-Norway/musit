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

package no.uio.musit.microservice.actor.domain

import no.uio.musit.microservices.common.domain.{BaseAddress, BaseMusitDomain}
import no.uio.musit.microservices.common.linking.domain.Link
import play.api.libs.json._
/**
  * Mapping table
  */
case class Actor(id: Long, actorname:String, links: Seq[Link]) extends BaseMusitDomain

/**
  * Domain Person
  */
case class Person(id:Long, fn:String, title:String, role:String, tel:String, web:String, email:String, links: Seq[Link]) extends BaseMusitDomain

/**
  * Domain Organization
  */
case class Organization(id:Long, fn:String, nickname:String, tel:String, web:String, latitude:Double, longitude:Double, links: Seq[Link]) extends BaseMusitDomain

/**
  * Address specialized for Organization
  */
case class OrganizationAddress(id:Long, organizationId:Long, addressType:String, streetAddress:String, locality:String, postalCode:String, countryName:String, links: Seq[Link]) extends BaseAddress

object Actor {
  def tupled = (Actor.apply _).tupled
  implicit val format = Json.format[Actor]
}

object Person {
  def tupled = (Person.apply _).tupled
  implicit val format = Json.format[Person]
}

object Organization {
  def tupled = (Organization.apply _).tupled
  implicit val format = Json.format[Organization]
}

object OrganizationAddress {
  def tupled = (OrganizationAddress.apply _).tupled
  implicit val format = Json.format[OrganizationAddress]
}



