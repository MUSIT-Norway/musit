package repositories.analysis.dao

import com.google.inject.{Inject, Singleton}
import models.analysis.events.AnalysisResults.AnalysisResult
import models.analysis.events._
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess, MusitValidationError}
import no.uio.musit.models.{EventId, MuseumCollection, MuseumId, ObjectUUID}
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.shared.dao.DbErrorHandlers

import scala.concurrent.Future

@Singleton
class AnalysisDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends AnalysisTables
    with DbErrorHandlers {

  val logger = Logger(classOf[AnalysisDao])

  import profile.api._

  private val noaction: DBIO[Unit] = DBIO.successful(())

  private def insertAnalysisAction(event: EventRow): DBIO[EventId] = {
    analysisTable returning analysisTable.map(_.id) += event
  }

  private def upsertResultAction(
      mid: MuseumId,
      id: EventId,
      result: AnalysisResult
  ): DBIO[Int] = {
    resultTable.insertOrUpdate(asResultTuple(mid, id, result))
  }

  private def insertAnalysisWithResultAction(
      mid: MuseumId,
      ae: AnalysisEvent,
      maybeRes: Option[AnalysisResult]
  ): DBIO[EventId] = {
    for {
      id <- insertAnalysisAction(asEventTuple(mid, ae))
      _  <- maybeRes.map(r => upsertResultAction(mid, id, r)).getOrElse(noaction)
    } yield id
  }

  private def insertChildEventsAction(
      mid: MuseumId,
      pid: EventId,
      events: Seq[Analysis]
  ): DBIO[Seq[EventId]] = {
    val batch = events.map { e =>
      insertAnalysisWithResultAction(mid, e.copy(partOf = Some(pid)), e.result)
    }
    DBIO.sequence(batch)
  }

  private def updateAction(
      mid: MuseumId,
      id: EventId,
      event: AnalysisEvent
  ): DBIO[Int] = analysisTable.filter(_.id === id).update(asEventTuple(mid, event))

  private def findByIdAction(
      mid: MuseumId,
      id: EventId,
      includeSample: Boolean
  ): DBIO[Option[AnalysisModuleEvent]] = {
    val q1 = analysisTable.filter(a => a.id === id && a.museumId === mid)
    val q2 = {
      if (includeSample) q1
      else q1.filter(_.typeId =!= SampleCreated.sampleEventTypeId)
    }

    q2.result.headOption.map { res =>
      res.flatMap(toAnalysisModuleEvent)
    }
  }

  private def resultForEventIdAction(
      mid: MuseumId,
      id: EventId
  ): DBIO[Option[AnalysisResult]] = {
    resultTable
      .filter(r => r.eventId === id && r.museumId === mid)
      .result
      .headOption
      .map { res =>
        res.flatMap(fromResultRow)
      }
  }

  private def listChildrenAction(
      mid: MuseumId,
      parentId: EventId
  ): DBIO[Seq[Analysis]] = {
    val query = analysisTable.filter { a =>
      a.partOf === parentId && a.museumId === mid
    } joinLeft resultTable on (_.id === _.eventId)

    query.result.map { res =>
      res.map { row =>
        toAnalysis(row._1)
          .flatMap(_.withResultAsOpt[Analysis](fromResultRow(row._2)))
          .get
      }
    }
  }

  private def listForObjectAction(
      mid: MuseumId,
      oid: ObjectUUID
  ): DBIO[Seq[AnalysisModuleEvent]] = {
    val query = analysisTable.filter { a =>
      a.objectUuid === oid && a.museumId === mid
    } joinLeft resultTable on (_.id === _.eventId)

    query.result.map { res =>
      res.flatMap { row =>
        toAnalysisModuleEvent(row._1).map {
          case ae: AnalysisEvent =>
            ae.withResultAsOpt(fromResultRow(row._2)).getOrElse(ae)
          case so: SampleCreated => so
        }
      }
    }
  }

  private def sampleIdsForObjectAction(
      mid: MuseumId,
      oid: ObjectUUID
  ): DBIO[Seq[ObjectUUID]] = {
    sampleObjTable
      .filter(s => s.originatedFrom === oid && s.museumId === mid)
      .map(_.id)
      .result
  }

  private def listAllForObjectAction(
      mid: MuseumId,
      oid: ObjectUUID,
      sampleIds: Seq[ObjectUUID]
  ): DBIO[Seq[AnalysisModuleEvent]] = {
    val setid = SampleCreated.sampleEventTypeId

    def checkObjectUuid(id: Rep[Option[ObjectUUID]]) = {
      if (sampleIds.nonEmpty) id === oid || (id inSet sampleIds)
      else id === oid
    }

    val qry = for {
      a <- analysisTable.filter(a => a.museumId === mid && checkObjectUuid(a.objectUuid))
      b <- analysisTable if a.partOf === b.id || (b.id === a.id && a.typeId === setid)
    } yield b

    val query = qry joinLeft resultTable on (_.id === _.eventId)

    query.result.map { res =>
      res.flatMap { row =>
        toAnalysisModuleEvent(row._1).map {
          case ae: AnalysisEvent =>
            ae.withResultAsOpt(fromResultRow(row._2)).getOrElse(ae)
          case so: SampleCreated => so
        }
      }
    }
  }

  /**
   * Write a single {{{Analysis}}} event to the DB.
   *
   * @param mid the MuseumId
   * @param a   The Analysis to persist.
   * @return eventually returns a MusitResult containing the EventId.
   */
  def insert(mid: MuseumId, a: Analysis): Future[MusitResult[EventId]] = {
    val action = insertAnalysisWithResultAction(mid, a, a.result)

    db.run(action.transactionally)
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"An unexpected error occurred inserting an analysis event"))
  }

  /**
   * Inserts an {{{AnalysisCollection}}} to the DB. The difference from inserting
   * a bunch of analysis' one by one is that; each Analysis row will get their,
   * partOf attribute set to the EventId of the bounding {{{AnalysisCollection}}}.
   *
   * @param mid the MuseumId
   * @param ac  The AnalysisCollection to persist.
   * @return eventually returns a MusitResult containing the EventId.
   */
  def insertCol(mid: MuseumId, ac: AnalysisCollection): Future[MusitResult[EventId]] = {
    val action = for {
      id   <- insertAnalysisWithResultAction(mid, ac.withoutChildren, ac.result)
      eids <- insertChildEventsAction(mid, id, ac.events)
    } yield id

    db.run(action.transactionally)
      .map(MusitSuccess.apply)
      .recover(
        nonFatal(s"An unexpected error occurred inserting an analysis event collection")
      )
  }

  /**
   * Performs an update action against the DB using the values in the provided
   * {{{AnalysisEvent}}} argument.
   *
   * @param mid the MuseumId
   * @param id  the EventId associated with the analysis event to update
   * @param ae  the AnalysisEvent to update
   * @return a result with an option of the updated event
   */
  def update(
      mid: MuseumId,
      id: EventId,
      ae: AnalysisEvent
  ): Future[MusitResult[Option[AnalysisEvent]]] = {
    val action = updateAction(mid, id, ae).transactionally

    db.run(action)
      .flatMap { numUpdated =>
        if (numUpdated == 1) {
          findAnalysisById(mid, id)
        } else {
          Future.successful {
            MusitValidationError(
              message = "Unexpected number of AnalysisEvent rows were updated.",
              expected = Option(1),
              actual = Option(numUpdated)
            )
          }
        }
      }
      .recover(nonFatal(s"An unexpected error occurred inserting an analysis event"))
  }

  /**
   * Locates a specific analysis module related event by its EventId.
   *
   * @param mid           the MuseumId to look for.
   * @param id            the EventId to look for.
   * @param includeSample Boolean flag to control which event types are returned.
   * @return eventually returns a MusitResult that might contain the AnalysisModuleEvent.
   */
  def findById(
      mid: MuseumId,
      id: EventId,
      includeSample: Boolean = true
  ): Future[MusitResult[Option[AnalysisModuleEvent]]] = {
    val query = for {
      maybeEvent <- findByIdAction(mid, id, includeSample)
      maybeRes   <- resultForEventIdAction(mid, id)
      children   <- listChildrenAction(mid, id)
    } yield {
      maybeEvent.flatMap {
        case a: Analysis =>
          Option(a.withResult(maybeRes))

        case ac: AnalysisCollection =>
          Option(ac.copy(events = children).withResult(maybeRes))

        case sc: SampleCreated =>
          if (includeSample) Option(sc) else None
      }
    }

    db.run(query)
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"An unexpected error occurred fetching event $id"))
  }

  /**
   * Same as findById, but will ensure that only the Analysis specific events
   * are returned.
   *
   * @param id The event ID to look for
   * @return the AnalysisEvent that was found or None
   */
  def findAnalysisById(
      mid: MuseumId,
      id: EventId
  ): Future[MusitResult[Option[AnalysisEvent]]] = {
    findById(mid, id, includeSample = false).map(_.map(_.flatMap {
      case ae: AnalysisEvent => Some(ae)
      case _                 => None
    }))
  }

  /**
   * Find all analysis events that are _part of_ an analysis container.
   *
   * Children can _only_ be of type {{{Analysis}}}.
   *
   * @param mid the MuseumId
   * @param id  The analysis container to find children for.
   * @return eventually a result with a list of analysis events and their results
   */
  def listChildren(mid: MuseumId, id: EventId): Future[MusitResult[Seq[Analysis]]] = {
    db.run(listChildrenAction(mid, id))
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"An unexpected error occurred fetching events partOf $id"))
  }

  /**
   * Locate all events related to the provided ObjectUUID and its derived samples.
   *
   * @param mid the MuseumId
   * @param oid The ObjectUUID to find analysis' for
   * @return eventually a result with a list of analysis events and their results
   */
  def findByCollectionObjectUUID(
      mid: MuseumId,
      oid: ObjectUUID
  ): Future[MusitResult[Seq[AnalysisModuleEvent]]] = {
    val eventsRes = for {
      derivedSampleIds <- db.run(sampleIdsForObjectAction(mid, oid))
      events           <- db.run(listAllForObjectAction(mid, oid, derivedSampleIds))
    } yield MusitSuccess(events)

    eventsRes.recover(
      nonFatal(s"An unexpected error occurred fetching events for object $oid")
    )
  }

  /**
   * Usefull method for locating the result for a specific analysis event.
   *
   * @param mid the MuseumId
   * @param id  the EventId of the analysis event to fetch the result for
   * @return a result that may or may not contain the AnalysisResult
   */
  def findResultFor(
      mid: MuseumId,
      id: EventId
  ): Future[MusitResult[Option[AnalysisResult]]] = {
    val q = resultTable
      .filter(r => r.eventId === id && r.museumId === mid)
      .result
      .headOption
      .map(fromResultRow)

    db.run(q)
      .map(MusitSuccess.apply)
      .recover(
        nonFatal(s"An unexpected error occurred looking up result for analysis $id")
      )
  }

  /**
   * Adds or updates a result for the analysis with the given EventId.
   *
   * @param mid the MuseumId.
   * @param id  The EventId of the analysis that has a new result.
   * @param res The AnalysisResult to add.
   * @return eventually a result with the EventId the saved result belongs to.
   */
  def upsertResult(
      mid: MuseumId,
      id: EventId,
      res: AnalysisResult
  ): Future[MusitResult[EventId]] = {
    val action = upsertResultAction(mid, id, res)

    db.run(action.transactionally)
      .flatMap { numUpdated =>
        val q = resultTable.filter(_.eventId === id).map(_.eventId)
        db.run(q.result.headOption).map {
          case Some(resId) =>
            MusitSuccess(resId)

          case None =>
            MusitValidationError(
              s"Could not find the result for $id that was just inserted"
            )
        }
      }
      .recover(
        nonFatal(s"An unexpected error occurred inserting a result to analysis $id")
      )
  }

  /**
   * Find all analysis events.
   *
   * @param mid The Museum id.
   * @return a collection of the events.
   */
  // scalastyle:off
  def findAnalysisEvents(
      mid: MuseumId,
      museumCollections: Seq[MuseumCollection]
  )(
      implicit currUser: AuthenticatedUser
  ): Future[MusitResult[Seq[AnalysisModuleEvent]]] = {

    type AEQuery = Query[AnalysisTable, AnalysisTable#TableElementType, Seq]

    def buildQuery(ids: Seq[EventId]): AEQuery = analysisTable.filter(_.id inSet ids)

    val musColFilter = {
      val cids = museumCollections.map(_.collection.id)
      if (currUser.hasGodMode) ""
      else
        s"""AND e2.NEW_COLLECTION_ID in (${cids.mkString(",")})"""
    }

    val eventIdQuery =
      sql"""
        SELECT DISTINCT e1.EVENT_ID FROM
          MUSARK_ANALYSIS.EVENT e1,
          (
            SELECT ee.PART_OF, so.ORIGINATED_OBJECT_UUID, mt.NEW_COLLECTION_ID
            FROM MUSARK_ANALYSIS.EVENT ee
              LEFT OUTER JOIN MUSARK_ANALYSIS.SAMPLE_OBJECT so
                ON ee.OBJECT_UUID = so.SAMPLE_UUID
              LEFT OUTER JOIN MUSIT_MAPPING.MUSITTHING mt
                ON ee.OBJECT_UUID = mt.MUSITTHING_UUID
                OR so.ORIGINATED_OBJECT_UUID = mt.MUSITTHING_UUID
            WHERE ee.MUSEUM_ID = ${mid.underlying}
              AND ee.TYPE_ID != 0
              AND ee.PART_OF IS NOT NULL
          ) e2
        WHERE e1.PART_OF IS NULL
        AND e1.TYPE_ID != 0
        AND e1.MUSEUM_ID = ${mid.underlying}
        AND e1.EVENT_ID = e2.PART_OF
        #${musColFilter}
      """.as[Long]

    db.run(eventIdQuery)
      .flatMap { eventIds =>
        if (eventIds.nonEmpty) {
          val eids = eventIds.map(EventId.fromLong).grouped(500)
          val query = eids.foldLeft[(Int, AEQuery)]((0, analysisTable)) {
            case (qry, ids) =>
              if (qry._1 == 0) (1, buildQuery(ids))
              else (qry._1 + 1, qry._2 unionAll buildQuery(ids))
          }
          db.run(query._2.result)
        } else {
          Future.successful(Vector.empty)
        }
      }
      .map(_.flatMap(toAnalysisModuleEvent))
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"An unexpected error occurred while fetching analysis events"))
  }

}
