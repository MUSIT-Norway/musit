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
 * Domain Person
 */
case class Person(id: Option[Long], fn: String, title: Option[String] = None, role: Option[String] = None, tel: Option[String] = None,
  web: Option[String] = None, email: Option[String] = None, dataportenId: Option[String] = None, links: Option[Seq[Link]]) extends BaseMusitDomain

/**
 * Domain Organization
 */
case class Organization(
  id: Option[Long],
  fn: String,
  nickname: String,
  tel: String,
  web: String,
  links: Option[Seq[Link]]
) extends BaseMusitDomain

/**
 * Address specialized for Organization
 */
case class OrganizationAddress(
  id: Option[Long],
  organizationId: Option[Long],
  addressType: String,
  streetAddress: String,
  locality: String,
  postalCode: String,
  countryName: String,
  latitude: Double,
  longitude: Double,
  links: Option[Seq[Link]]
) extends BaseAddress

object Person {
  val tupled = (Person.apply _).tupled
  implicit val format = Json.format[Person]
}

object Organization {
  val tupled = (Organization.apply _).tupled
  implicit val format = Json.format[Organization]
}

object OrganizationAddress {
  val tupled = (OrganizationAddress.apply _).tupled
  implicit val format = Json.format[OrganizationAddress]
}

