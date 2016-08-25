///*
// * MUSIT is a museum database to archive natural and cultural history data.
// * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
// *
// * This program is free software; you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation; either version 2 of the License,
// * or any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License along
// * with this program; if not, write to the Free Software Foundation, Inc.,
// * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
// */
//
//package no.uio.musit.microservice.storagefacility.domain.event.dto
//
//trait Dto
//
//case class EnvRequirementDto(
//  id: Option[Long],
//  temperature: Option[Int],
//  tempInterval: Option[Int],
//  airHumidity: Option[Int],
//  airHumInterval: Option[Int],
//  hypoxicAir: Option[Int],
//  hypoxicInterval: Option[Int],
//  cleaning: Option[String],
//  light: Option[String]
//) extends Dto
//
//case class ObservationFromToDto(
//  id: Option[Long],
//  from: Option[Double],
//  to: Option[Double]
//) extends Dto
//
//// Note: The eventId is only used during writing to the database, it is
//// "None-ed out" after having been read from the database, to prevent it from
//// showing up in json.
//case class LivssyklusDto(
//  eventId: Option[Long],
//  livssyklus: Option[String],
//  antall: Option[Int]
//) extends Dto
//
//case class ObservationSkadedyrDto(livssykluser: Seq[LivssyklusDto]) extends Dto
//
//object ControlSpecificDtoSpec {
//  val customFieldsSpec = CustomFieldsSpec().defineRequiredBoolean("ok")
//}
//
