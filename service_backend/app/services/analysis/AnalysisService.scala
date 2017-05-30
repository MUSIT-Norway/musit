package services.analysis

import com.google.inject.Inject
import models.analysis.events.AnalysisResults.AnalysisResult
import models.analysis.events.SaveCommands.{
  SaveAnalysis,
  SaveAnalysisCollection,
  SaveAnalysisEventCommand
}
import models.analysis.events._
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models.{CollectionUUID, EventId, MuseumId, ObjectUUID}
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.analysis.dao.{AnalysisDao, AnalysisTypeDao}

import scala.concurrent.Future

class AnalysisService @Inject()(
    val analysisDao: AnalysisDao,
    val typeDao: AnalysisTypeDao
) {

  val logger = Logger(classOf[AnalysisService])

  /**
   * Return all AnalysisTypes that exist in the system
   */
  def getAllTypes: Future[MusitResult[Seq[EnrichedAnalysisType]]] = {
    typeDao.all.map(_.map(EnrichedAnalysisType.fromAnalysisTypes))
  }

  def getTypesFor(cat: Option[Category], coll: Option[CollectionUUID])(
      implicit currUser: AuthenticatedUser
  ): Future[MusitResult[Seq[EnrichedAnalysisType]]] = {
    typeDao.allFor(cat, coll).map(_.map(EnrichedAnalysisType.fromAnalysisTypes))
  }

  /**
   * Return the AnalyisType associated with the given AnalysisTypeId
   */
  private[services] def getType(
      tid: AnalysisTypeId
  ): Future[MusitResult[Option[AnalysisType]]] = typeDao.findById(tid)

  /**
   * Add a new AnalysisEvent.
   */
  def add(ae: SaveAnalysisEventCommand)(
      implicit currUser: AuthenticatedUser
  ): Future[MusitResult[EventId]] = {
    ae match {
      case a: SaveAnalysis            => addAnalysis(a)
      case ac: SaveAnalysisCollection => addAnalysisCollection(ac)
    }
  }

  /**
   * Helper method specifically for adding an Analysis.
   */
  private def addAnalysis(
      a: SaveAnalysis
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[EventId]] = {
    val analysis = a.asDomain
    val res = for {
      mat       <- MusitResultT(getType(analysis.analysisTypeId))
      validated <- MusitResultT(AnalysisEvent.validateAttributes(analysis, mat))
      eid       <- MusitResultT(analysisDao.insert(validated))
    } yield eid

    res.value
  }

  /**
   * Helper method specifically for adding an AnalysisCollection.
   */
  private def addAnalysisCollection(
      sac: SaveAnalysisCollection
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[EventId]] = {
    val analysisCol = sac.asDomain

    val res = for {
      mat       <- MusitResultT(getType(sac.analysisTypeId))
      validated <- MusitResultT(AnalysisEvent.validateAttributes(analysisCol, mat))
      eid       <- MusitResultT(analysisDao.insertCol(analysisCol))
    } yield eid

    res.value
  }

  /**
   * Add an AnalysisResult to the AnalysisEvent with the given EventId.
   */
  def addResult(
      eid: EventId,
      res: AnalysisResult
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[EventId]] = {
    updateResult(eid, res)
  }

  /**
   * Update the AnalysisResult belonging to the given EventId
   */
  def updateResult(
      eid: EventId,
      res: AnalysisResult
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[EventId]] = {
    MusitResultT(analysisDao.findResultFor(eid)).flatMap { orig =>
      val ar = orig.map { o =>
        res.withRegisteredBy(o.registeredBy).withtRegisteredDate(o.registeredDate)
      }.getOrElse {
        val now = Some(dateTimeNow)
        res.withRegisteredBy(Some(currUser.id)).withtRegisteredDate(now)
      }
      MusitResultT(analysisDao.upsertResult(eid, ar))
    }.value
  }

  /**
   * Update an AnalysisEvent with the content of the given SaveAnalysisEventCommand.
   */
  def update(
      mid: MuseumId,
      eid: EventId,
      ae: SaveAnalysisEventCommand
  )(
      implicit currUser: AuthenticatedUser
  ): Future[MusitResult[Option[AnalysisEvent]]] = {
    val res = for {
      maybeEvent <- MusitResultT(analysisDao.findAnalysisById(eid))
      maybeUpdated <- MusitResultT(
                       maybeEvent.map { e =>
                         val u = SaveAnalysisEventCommand.updateDomain(ae, e)
                         analysisDao.update(eid, u)
                       }.getOrElse(Future.successful(MusitSuccess(None)))
                     )
    } yield maybeUpdated

    res.value
  }

  /**
   * Locate the event with the given EventId.
   */
  def findById(id: EventId): Future[MusitResult[Option[AnalysisModuleEvent]]] = {
    analysisDao.findById(id)
  }

  /**
   * Fetch all children (which are all instances of Analysis) for the
   * event with the given EventId.
   */
  def childrenFor(id: EventId): Future[MusitResult[Seq[Analysis]]] = {
    analysisDao.listChildren(id)
  }

  /**
   * Locate all events associated with the given ObjectUUID.
   */
  def findByObject(oid: ObjectUUID): Future[MusitResult[Seq[AnalysisModuleEvent]]] = {
    analysisDao.findByObjectUUID(oid)
  }

}
