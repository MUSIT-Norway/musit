package services.loan

import com.google.inject.Inject
import models.loan.event.{LoanEvent, ObjectsLent}
import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.models.{EventId, MuseumId}
import repositories.loan.dao.LoanDao

import scala.concurrent.Future

class LoanService @Inject()(loanDao: LoanDao) {

  def createLoan(mid: MuseumId, ol: ObjectsLent): Future[MusitResult[EventId]] =
    loanDao.insertLentObjectEvent(mid, ol)

  def findActiveLoans(mid: MuseumId): Future[MusitResult[Seq[LoanEvent]]] =
    loanDao.findActiveLoanEvents(mid)

}
