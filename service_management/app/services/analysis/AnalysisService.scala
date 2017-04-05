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
  def getAllTypes: Future[MusitResult[Seq[AnalysisType]]] = typeDao.all

  /**
   * Return all AnalysisTypes "tagged" with the given Category
   */
  def getTypesFor(c: Category): Future[MusitResult[Seq[AnalysisType]]] = {
    typeDao.allForCategory(c)
  }

  /**
   * Return all AnalysisTypes associated with the given CollectionUUID. The
   * result also includes AnalysisTypes that aren't associated with _any_
   * CollectionUUIDs.
   */
  def getTypesFor(id: CollectionUUID): Future[MusitResult[Seq[AnalysisType]]] = {
    typeDao.allForCollection(id)
  }

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
    val analysis = a.asDomain.copy(
      registeredBy = Some(currUser.id),
      registeredDate = Some(dateTimeNow)
    )
    analysisDao.insert(analysis)
  }

  /**
   * Helper method specifically for adding an AnalysisCollection.
   */
  private def addAnalysisCollection(
      ac: SaveAnalysisCollection
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[EventId]] = {
    val now = Some(dateTimeNow)
    val d   = ac.asDomain
    val acol = d.copy(
      registeredBy = Some(currUser.id),
      registeredDate = now,
      events = d.events.map(
        _.copy(
          registeredBy = Some(currUser.id),
          registeredDate = now
        )
      )
    )
    analysisDao.insertCol(acol)
  }

  /**
   * Add an AnalysisResult to the AnalysisEvent with the given EventId.
   */
  def addResult(
      eid: EventId,
      res: AnalysisResult
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[Long]] = {
    val now = Some(dateTimeNow)
    val ar  = res.withRegisteredBy(Some(currUser.id)).withtRegisteredDate(now)
    analysisDao.upsertResult(eid, ar)
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
      maybeEvent <- MusitResultT(findById(eid))
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
   * Locate the AnalysisEvent with the given EventId.
   */
  def findById(id: EventId): Future[MusitResult[Option[AnalysisEvent]]] = {
    analysisDao.findById(id)
  }

  /**
   * Fetch all children (which are all instances of Analysis) for the
   * AnalysisEvent with the given EventId.
   */
  def childrenFor(id: EventId): Future[MusitResult[Seq[Analysis]]] = {
    analysisDao.listChildren(id)
  }

  /**
   * Locate all AnalysisEvents associated with the given ObjectUUID.
   */
  def findByObject(oid: ObjectUUID): Future[MusitResult[Seq[AnalysisEvent]]] = {
    analysisDao.findByObjectUUID(oid)
  }

}
