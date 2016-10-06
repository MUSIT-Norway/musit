package models.dto

import models.MusitThing

/**
 * Created by jarle on 06.10.16.
 */
case class MusitThingDto(museumId: Int, id: Long, museumNo: String, museumNoAsNumber: Option[Long], subNo: String, term: String)

object MusitThingDto {

  implicit def toDomain(x: MusitThingDto): MusitThing =
    MusitThing(
      museumNo = x.museumNo,
      subNo = x.subNo,
      term = x.term
    )
}

