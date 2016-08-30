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
//import no.uio.musit.microservice.storagefacility.domain.event.EventTypeId
//import no.uio.musit.microservices.common.extensions.OptionExtensions._
//import no.uio.musit.microservices.common.linking.domain.Link
//
///**
// * Created by jstabel on 7/7/16.
// */
//
//case class BaseEventDto(
//  id: Option[Long],
//  links: Option[Seq[Link]],
//  eventType: EventTypeId,
//  note: Option[String],
//  //  relatedSubEvents: Seq[RelatedEvents],
//  partOf: Option[Long],
//  valueLong: Option[Long] = None,
//  valueString: Option[String] = None,
//  valueDouble: Option[Double] = None
//)
//
//{
//
//  def getOptBool = valueLong match {
//    case Some(1) => Some(true)
//    case Some(0) => Some(false)
//    case None => None
//    case n => throw new Exception(s"Boolean value encoded as an opt integer should be either None, 0 or 1, not $n.")
//    //If this happens, we have a bug in our code!
//  }
//
//  def getBool = getOptBool.getOrFail("Missing required custom boolean value")
//
//  def setBool(value: Boolean) = this.copy(valueLong = Some(if (value) 1 else 0))
//
//  def getOptString: Option[String] = valueString
//
//  def getString: String = getOptString.getOrFail("Missing required custom string value")
//
//  def setString(value: String) = this.copy(valueString = Some(value))
//
//  def setOptString(value: Option[String]) = {
//    value match {
//      case Some(s) => setString(s)
//      case None => this.copy(valueString = None)
//    }
//  }
//
//  def getOptDouble: Option[Double] = valueDouble
//
//  def getDouble: Double = getOptDouble.getOrFail("Missing required custom double value")
//
//  def setDouble(value: Double) = this.copy(valueDouble = Some(value))
//
//  def setOptDouble(value: Option[Double]) = {
//    value match {
//      case Some(s) => setDouble(s)
//      case None => this.copy(valueDouble = None)
//    }
//  }
//
//}
