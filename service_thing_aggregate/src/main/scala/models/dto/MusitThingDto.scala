package models.dto

import models.MusitThing

/**
 * Created by jarle on 06.10.16.
 */
case class MusitThingDto(museumId: Int, id: Option[Long], museumNo: String, museumNoAsNumber: Option[Long], subNo: Option[String], subNoAsNumber: Option[Long], term: String)

object MusitThingDto {

  implicit def toDomain(x: MusitThingDto): MusitThing =
    MusitThing(
      museumNo = x.museumNo,
      subNo = x.subNo,
      term = x.term
    )

  val regExp = """\A\D*(\d+)\D*\z""".r
  //  val regExp = """\A(?>\D*)(\d+)\D*\z""".r

  /** The number part of a museumNo */
  def museumNoNumberPart(museumNo: String): Option[Long] = {
    val optM = regExp.findFirstMatchIn(museumNo)

    optM.map{m=>
      assert(m.groupCount==1) //This regular expression is designed to only return one group
      m.group(1).toLong //Per def of this re, this should always be possible (within reasonable length of museumNo!)
                        // and never throw any exceptions.
    }
  }

  def subNoNumberPart(subNo: String): Option[Long] = {
    museumNoNumberPart(subNo)
  }



  implicit def fromDomain(museumId: Int, x: MusitThing): MusitThingDto =
    MusitThingDto(
      id = None,
      museumId = museumId,
      museumNo = x.museumNo,
      museumNoAsNumber = museumNoNumberPart(x.museumNo),
      subNo = x.subNo,
      subNoAsNumber = x.subNo.flatMap(subNoNumberPart),
      term = x.term
    )
}

