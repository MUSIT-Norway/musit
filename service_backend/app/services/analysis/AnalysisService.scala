package services.analysis

import com.google.inject.Inject
import models.analysis.events.AnalysisResults.AnalysisResult
import models.analysis.events.SaveCommands.{
  SaveAnalysis,
  SaveAnalysisCollection,
  SaveAnalysisEventCommand
}
import models.analysis.events._
import no.uio.musit.MusitResults.{
  MusitError,
  MusitResult,
  MusitSuccess,
  MusitValidationError
}
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models._
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
  def add(mid: MuseumId, ae: SaveAnalysisEventCommand)(
      implicit currUser: AuthenticatedUser
  ): Future[MusitResult[Option[AnalysisModuleEvent]]] = {
    val eventuallyAdd = ae match {
      case a: SaveAnalysis            => addAnalysis(mid, a)
      case ac: SaveAnalysisCollection => addAnalysisCollection(mid, ac)
    }

    (for {
      added <- MusitResultT(eventuallyAdd)
      a     <- MusitResultT(findById(mid, added))
    } yield a).value
  }

  /**
   * Helper method specifically for adding an Analysis.
   */
  private def addAnalysis(
      mid: MuseumId,
      a: SaveAnalysis
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[EventId]] = {
    val analysis = a.asDomain
    val res = for {
      mat       <- MusitResultT(getType(analysis.analysisTypeId))
      validated <- MusitResultT(AnalysisEvent.validateAttributes(analysis, mat))
      eid       <- MusitResultT(analysisDao.insert(mid, validated))
    } yield eid

    res.value
  }

  /**
   * Helper method specifically for adding an AnalysisCollection.
   */
  private def addAnalysisCollection(
      mid: MuseumId,
      sac: SaveAnalysisCollection
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[EventId]] = {
    val analysisCol = sac.asDomain

    val res = for {
      mat       <- MusitResultT(getType(sac.analysisTypeId))
      validated <- MusitResultT(AnalysisEvent.validateAttributes(analysisCol, mat))
      eid       <- MusitResultT(analysisDao.insertCol(mid, analysisCol))
    } yield eid

    res.value
  }

  /**
   * Add an AnalysisResult to the AnalysisEvent with the given EventId.
   */
  def addResult(
      mid: MuseumId,
      eid: EventId,
      res: AnalysisResult
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[EventId]] = {
    updateResult(mid, eid, res)
  }

  /**
   * Update the AnalysisResult belonging to the given EventId
   */
  def updateResult(
      mid: MuseumId,
      eid: EventId,
      res: AnalysisResult
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[EventId]] = {
    MusitResultT(analysisDao.findResultFor(mid, eid)).flatMap { orig =>
      val ar = enrichResult(res, orig)
      MusitResultT(analysisDao.upsertResult(mid, eid, ar))
    }.value
  }

  private[this] def enrichResult(
      res: AnalysisResult,
      orig: Option[AnalysisResult]
  )(implicit currUser: AuthenticatedUser): AnalysisResult = {
    orig.map { o =>
      res.withRegisteredBy(o.registeredBy).withRegisteredDate(o.registeredDate)
    }.getOrElse {
      val now = Some(dateTimeNow)
      res.withRegisteredBy(Some(currUser.id)).withRegisteredDate(now)
    }
  }

  private[this] def enrichImportResults(
      eid: EventId,
      res: AnalysisResultImport,
      orig: AnalysisCollection
  )(implicit currUser: AuthenticatedUser): Seq[(EventId, AnalysisResult)] = {
    val now     = Some(dateTimeNow)
    val results = Seq.newBuilder[(EventId, AnalysisResult)]
    // Add the enriched result for the AnalysisCollection
    results += (eid -> enrichResult(res.collectionResult, orig.result))
    // Add the enriched results for Analyses on the AnalysisCollection
    results ++= res.objectResults.flatMap { rfoe =>
      orig.events.find(_.id.contains(rfoe.eventId)).map { analysis =>
        rfoe.eventId -> enrichResult(rfoe.result, analysis.result)
      }
    }
    results.result()
  }

  def updateResults(
      mid: MuseumId,
      eid: EventId,
      res: AnalysisResultImport
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[Unit]] = {
    // Checks each result and returns a list of results that didn't match
    // the expected eventId + objectId
    def validate(ac: AnalysisCollection): Seq[ResultForObjectEvent] = {
      val invalid = Seq.newBuilder[ResultForObjectEvent]
      res.objectResults.foreach { rfoe =>
        val valid = ac.events.exists { analysis =>
          analysis.id.contains(rfoe.eventId) &&
          analysis.objectId.contains(rfoe.objectId)
        }
        if (!valid) invalid += rfoe
      }
      invalid.result()
    }

    MusitResultT(findById(mid, eid)).flatMap {
      case Some(ac: AnalysisCollection) =>
        val invalid = validate(ac)
        if (invalid.isEmpty) {
          val results = enrichImportResults(eid, res, ac)
          MusitResultT(analysisDao.upsertResults(mid, results))
        } else {
          MusitResultT.failed[Unit](
            MusitValidationError(
              s"The results with the following objectId+eventId could not" +
                s" be found on the AnalysisCollection: " +
                s"${invalid.map(i => s"(${i.eventId} - ${i.objectId}")})"
            )
          )
        }

      case Some(bad) =>
        MusitResultT.failed[Unit](
          MusitValidationError(
            s"Expected an AnalysisCollection, but found an ${bad.getClass}"
          )
        )

      case None =>
        MusitResultT.failed[Unit](
          MusitValidationError(s"AnalysisCollection with id $eid could not be found")
        )
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
      maybeEvent <- MusitResultT(analysisDao.findAnalysisById(mid, eid))
      maybeUpdated <- MusitResultT(
                       maybeEvent.map { e =>
                         val u = SaveAnalysisEventCommand.updateDomain(ae, e)
                         analysisDao.update(mid, eid, u)
                       }.getOrElse(Future.successful(MusitSuccess(None)))
                     )
    } yield maybeUpdated

    res.value
  }

  /**
   * Locate the event with the given EventId.
   */
  def findById(
      mid: MuseumId,
      id: EventId
  ): Future[MusitResult[Option[AnalysisModuleEvent]]] = {
    analysisDao.findById(mid, id)
  }

  /**
   * Fetch all children (which are all instances of Analysis) for the
   * event with the given EventId.
   */
  def childrenFor(mid: MuseumId, id: EventId): Future[MusitResult[Seq[Analysis]]] = {
    analysisDao.listChildren(mid, id)
  }

  /**
   * Locate all events associated with the given ObjectUUID.
   */
  def findByObject(
      mid: MuseumId,
      oid: ObjectUUID
  ): Future[MusitResult[Seq[AnalysisModuleEvent]]] = {
    analysisDao.findByCollectionObjectUUID(mid, oid)
  }

  /**
   * Find analysis collection events
   */
  def findAnalysisEvents(
      mid: MuseumId,
      museumCollections: Seq[MuseumCollection]
  )(
      implicit currUser: AuthenticatedUser
  ): Future[MusitResult[Seq[AnalysisModuleEvent]]] = {
    analysisDao.findAnalysisEvents(mid, museumCollections)
  }

}
