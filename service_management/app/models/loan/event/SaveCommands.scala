package models.loan.event

import models.loan.LoanEventTypes.ObjectLentType
import no.uio.musit.models.{ExternalRef, ObjectUUID}
import org.joda.time.DateTime
import play.api.libs.json.{Format, Json}

object SaveCommands {

  case class CreateLoanCommand(
      externalRef: Option[ExternalRef],
      note: Option[String],
      returnDate: DateTime,
      objects: Seq[ObjectUUID]
  ) {
    def toDomain: ObjectsLent = {
      ObjectsLent(
        id = None,
        loanType = ObjectLentType,
        eventDate = None,
        registeredBy = None,
        registeredDate = None,
        partOf = None,
        externalRef = externalRef,
        note = note,
        returnDate = returnDate,
        objects = objects
      )
    }
  }

  object CreateLoanCommand {
    implicit val format: Format[CreateLoanCommand] = Json.format[CreateLoanCommand]
  }

}
