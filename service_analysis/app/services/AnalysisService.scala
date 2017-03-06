package services

import com.google.inject.Inject
import models.events.AnalysisResults.AnalysisResult
import models.events.{Analysis, AnalysisCollection, AnalysisType, Category}
import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.models.{CollectionUUID, EventId, ObjectUUID}
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

  def add(a: Analysis): Future[MusitResult[EventId]] = {
    analysisDao.insert(a)
  }

  def add(ac: AnalysisCollection): Future[MusitResult[EventId]] = {
    analysisDao.insertCol(ac)
  }

  def addResult(eid: EventId, res: AnalysisResult): Future[MusitResult[Long]] = {
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
