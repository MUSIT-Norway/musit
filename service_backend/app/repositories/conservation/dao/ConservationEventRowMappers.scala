package repositories.conservation.dao

import models.conservation.events.{ConservationEvent, ConservationModuleEvent}
import no.uio.musit.models.{EventId, MuseumId, MusitUUID}
import no.uio.musit.repositories.events.EventRowMappers
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json, Reads, Writes}

trait ConservationEventRowMappers extends EventRowMappers[ConservationModuleEvent] {
  self: ConservationEventTableProvider =>

  override protected def asRow(
      mid: MuseumId,
      e: ConservationModuleEvent
  )(
      implicit jsw: Writes[ConservationModuleEvent],
      currUsr: AuthenticatedUser
  ): EventRow = {
    val js = Json.toJson[ConservationModuleEvent](e)

    (
      e.id,
      e.eventTypeId,
      mid,
      e.registeredBy.getOrElse(currUsr.id),
      e.registeredDate.getOrElse(dateTimeNow),
      e.doneBy,
      e.doneDate,
      e.updatedDate,
      e.partOf,
      e.affectedThing.map(_.asString),
      e.note,
      e.caseNumber,
      js
    )
  }

  override protected def fromRow(
      maybeEventId: Option[EventId],
      maybeDoneDate: Option[DateTime],
      maybeAffectedThing: Option[MusitUUID],
      rowAsJson: JsValue
  )(implicit jsr: Reads[ConservationModuleEvent]) = {
    // println(Json.prettyPrint(rowAsJson))
    /*Json.fromJson[ConservationModuleEvent](rowAsJson).asOpt.map { row =>
      row
        .withId(maybeEventId)
        .withDoneDate(maybeDoneDate)
        .withAffectedThing(maybeAffectedThing)
        .asInstanceOf[ConservationModuleEvent]
    }*/
    Json.fromJson[ConservationModuleEvent](rowAsJson).asEither match {
      case Right(row) =>
        val x = row
          .withId(maybeEventId)
          .withDoneDate(maybeDoneDate)
          .withAffectedThing(maybeAffectedThing)
          .asInstanceOf[ConservationModuleEvent]

        Some(x)

      case Left(err) =>
        println(err)
        assert(false, "Noe gikk galt i fromRow. " + err)
        None

    }
  }

  protected def toConservationEvent(
      maybeEventId: Option[EventId],
      maybeDoneDate: Option[DateTime],
      maybeAffectedThing: Option[MusitUUID],
      rowAsJson: JsValue
  ): Option[ConservationEvent] =
    fromRow(maybeEventId, maybeDoneDate, maybeAffectedThing, rowAsJson).flatMap {
      case a: ConservationEvent => Some(a)
      case _                    => None
    }

}
