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

package models.dto

import models.{MuseumNo, MusitObject, SubNo}

case class MusitObjectDto(
  museumId: Int,
  id: Option[Long],
  museumNo: String,
  museumNoAsNumber: Option[Long],
  subNo: Option[String],
  subNoAsNumber: Option[Long],
  term: String
)

object MusitObjectDto {

  def toDomain(x: MusitObjectDto): MusitObject =
    MusitObject(
      museumNo = MuseumNo(x.museumNo),
      subNo = x.subNo.map(SubNo.apply),
      term = x.term
    )

  def fromDomain(museumId: Int, x: MusitObject): MusitObjectDto =
    MusitObjectDto(
      id = None,
      museumId = museumId,
      museumNo = x.museumNo.value,
      museumNoAsNumber = x.museumNo.asNumber,
      subNo = x.subNo.map(_.value),
      subNoAsNumber = x.subNo.flatMap(_.asNumber),
      term = x.term
    )
}

