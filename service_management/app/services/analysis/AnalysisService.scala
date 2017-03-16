package services.analysis

import com.google.inject.Inject
import models.analysis.events.AnalysisResults.AnalysisResult
import models.analysis.events._
import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.models.{CollectionUUID, EventId, ObjectUUID}
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import play.api.Logger
import repositories.analysis.dao.{AnalysisDao, AnalysisTypeDao}
import repositories.analysis.dao.AnalysisTypeDao

import scala.concurrent.Future

class AnalysisService @Inject() (
    val analysisDao: AnalysisDao,
    val typeDao: AnalysisTypeDao
) {

  val logger = Logger(classOf[AnalysisService])

  def getAllTypes: Future[MusitResult[Seq[AnalysisType]]] = typeDao.all

  def getTypesFor(c: Category): Future[MusitResult[Seq[AnalysisType]]] = {
    typeDao.allForCategory(c)
  }

  def getTypesFor(id: CollectionUUID): Future[MusitResult[Seq[AnalysisType]]] = {
    typeDao.allForCollection(id)
  }

  def add(ae: AnalysisEvent)(
    implicit
    currUser: AuthenticatedUser
  ): Future[MusitResult[EventId]] = {
    ae match {
      case a: Analysis => addAnalysis(a)
      case ac: AnalysisCollection => addAnalysisCollection(ac)
    }
  }

  def addAnalysis(
    a: Analysis
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[EventId]] = {
    val analysis = a.copy(
      registeredBy = Some(currUser.id),
      registeredDate = Some(dateTimeNow)
    )
    analysisDao.insert(analysis)
  }

  def addAnalysisCollection(
    ac: AnalysisCollection
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[EventId]] = {
    val now = Some(dateTimeNow)
    val acol = ac.copy(
      registeredBy = Some(currUser.id),
      registeredDate = now,
      events = ac.events.map(_.copy(
        registeredBy = Some(currUser.id),
        registeredDate = now
      ))
    )
    analysisDao.insertCol(acol)
  }

  def addResult(
    eid: EventId,
    res: AnalysisResult
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[Long]] = {
    val now = Some(dateTimeNow)
    val ar = res.withRegisteredBy(Some(currUser.id)).withtRegisteredDate(now)
    analysisDao.insertResult(eid, ar)
  }

  def findById(id: EventId): Future[MusitResult[Option[AnalysisEvent]]] = {
    analysisDao.findById(id)
  }

  def childrenFor(id: EventId): Future[MusitResult[Seq[Analysis]]] = {
    analysisDao.listChildren(id)
  }

  def findByObject(oid: ObjectUUID): Future[MusitResult[Seq[AnalysisEvent]]] = {
    analysisDao.findByObjectUUID(oid)
  }

}
