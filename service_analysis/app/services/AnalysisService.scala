package services

import com.google.inject.Inject
import models.events.AnalysisResults.AnalysisResult
import models.events.{Analysis, AnalysisCollection, AnalysisType, Category}
import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.models.{CollectionUUID, EventId, ObjectUUID}
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import play.api.Logger
import repositories.dao.{AnalysisDao, AnalysisTypeDao}

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

  def add(
    a: Analysis
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[EventId]] = {
    val analysis = a.copy(
      registeredBy = Some(currUser.id),
      registeredDate = Some(dateTimeNow)
    )
    analysisDao.insert(analysis)
  }

  def add(
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
    analysisDao.insertResult(eid, res)
  }

  def findById(id: EventId): Future[MusitResult[Option[Analysis]]] = {
    analysisDao.findById(id)
  }

  def childrenFor(id: EventId): Future[MusitResult[Seq[Analysis]]] = {
    analysisDao.listChildren(id)
  }

  def findByObject(oid: ObjectUUID): Future[MusitResult[Seq[Analysis]]] = {
    analysisDao.findByObjectUUID(oid)
  }

}
