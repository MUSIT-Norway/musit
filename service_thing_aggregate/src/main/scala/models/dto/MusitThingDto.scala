package models.dto

import models.MusitThing

/**
 * Created by jarle on 06.10.16.
 */
case class MusitThingDto(museumId: Int, id: Option[Long], museumNo: String, museumNoAsNumber: Option[Long], subNo: Option[String], term: String)

object MusitThingDto {

  implicit def toDomain(x: MusitThingDto): MusitThing =
    MusitThing(
      museumNo = x.museumNo,
      subNo = x.subNo,
      term = x.term
    )

  def calcMuseumNoAsNumber(museumNo: String): Option[Long] = {
    None //TODO!
  }

  implicit def fromDomain(museumId: Int, x: MusitThing): MusitThingDto =
    MusitThingDto(
      id = None,
      museumId = museumId,
      museumNo = x.museumNo,
      museumNoAsNumber = calcMuseumNoAsNumber(x.museumNo),
      subNo = x.subNo,
      term = x.term
    )


}

