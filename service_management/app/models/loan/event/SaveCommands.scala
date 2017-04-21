package models.loan.event

import models.loan.LoanEventTypes.ObjectLentType
import no.uio.musit.models.{CaseNumbers, ObjectUUID}
import no.uio.musit.time.dateTimeNow
import org.joda.time.DateTime
import play.api.libs.json.{Format, Json}

object SaveCommands {

  case class CreateLoanCommand(
      caseNumbers: Option[CaseNumbers],
      note: Option[String],
      returnDate: DateTime,
      objects: Seq[ObjectUUID]
  ) {
    def toDomain: ObjectsLent = {
      ObjectsLent(
        id = None,
        loanType = ObjectLentType,
        eventDate = Some(dateTimeNow),
        registeredBy = None,
        registeredDate = None,
        partOf = None,
        caseNumbers = caseNumbers,
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
