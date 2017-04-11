package services.loan

import com.google.inject.Inject
import models.loan.event.{LoanEvent, ObjectsLent}
import no.uio.musit.MusitResults.{MusitResult, MusitValidationError}
import no.uio.musit.models.{EventId, MuseumId}
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import repositories.loan.dao.LoanDao

import scala.concurrent.Future

class LoanService @Inject()(loanDao: LoanDao) {

  def createLoan(mid: MuseumId, ol: ObjectsLent)(
      implicit currUser: AuthenticatedUser
  ): Future[MusitResult[EventId]] = {
    if (ol.objects.isEmpty) {
      Future.successful(MusitValidationError("Can't create a loan without any objects"))
    } else {
      val toInsert = ol.copy(
        registeredBy = Some(currUser.id),
        registeredDate = Some(dateTimeNow)
      )
      loanDao.insertLentObjectEvent(mid, toInsert)
    }
  }

  def findActiveLoans(mid: MuseumId): Future[MusitResult[Seq[LoanEvent]]] =
    loanDao.findActiveLoanEvents(mid)

}
