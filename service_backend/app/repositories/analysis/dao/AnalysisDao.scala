package repositories.analysis.dao

import com.google.inject.{Inject, Singleton}
import models.analysis.events.AnalysisResults.AnalysisResult
import models.analysis.events._
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess, MusitValidationError}
import no.uio.musit.models.{EventId, MuseumCollection, MuseumId, ObjectUUID}
import no.uio.musit.repositories.events.EventActions
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AnalysisDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext
) extends AnalysisEventTableProvider
    with AnalysisTables
    with EventActions
    with AnalysisEventRowMappers {

  val logger = Logger(classOf[AnalysisDao])

  import profile.api._

  private def upsertResultAction(
      mid: MuseumId,
      id: EventId,
      result: AnalysisResult
  ): DBIO[Int] = {
    resultTable.insertOrUpdate(asResultRow(mid, id, result))
  }

  private def insertAnalysisWithResultAction(
      mid: MuseumId,
      ae: AnalysisEvent,
      maybeRes: Option[AnalysisResult]
  )(implicit currUsr: AuthenticatedUser): DBIO[EventId] = {
    insertEventWithAdditionalAction(mid, ae)(asRow) { (event, eid) =>
      maybeRes.map(res => upsertResultAction(mid, eid, res)).getOrElse(noaction)
    }
  }

  private def insertChildEventsAction(
      mid: MuseumId,
      pid: EventId,
      events: Seq[Analysis]
  )(implicit currUsr: AuthenticatedUser): DBIO[Seq[EventId]] = {
    insertBatchWithAdditionalAction(mid, events.map(_.copy(partOf = Some(pid))))(asRow) {
      (event, eid) =>
        event.result.map(res => upsertResultAction(mid, eid, res)).getOrElse(noaction)
    }
  }

  private def updateAction(
      mid: MuseumId,
      id: EventId,
      event: AnalysisEvent
  )(implicit currUsr: AuthenticatedUser): DBIO[Int] =
    eventTable.filter(_.eventId === id).update(asRow(mid, event))

  private def findByIdAction(
      mid: MuseumId,
      id: EventId,
      includeSample: Boolean
  )(implicit currUsr: AuthenticatedUser): DBIO[Option[AnalysisModuleEvent]] = {
    val q1 = eventTable.filter(a => a.eventId === id && a.museumId === mid)
    val q2 = {
      if (includeSample) q1
      else q1.filter(_.eventTypeId =!= SampleCreated.sampleEventTypeId)
    }

    q2.result.headOption.map(
      _.flatMap { row =>
        fromRow(row._1, row._7, row._10.flatMap(ObjectUUID.fromString), row._14)
      }
    )
  }

  private def resultForEventIdAction(
      mid: MuseumId,
      id: EventId
  )(implicit currUsr: AuthenticatedUser): DBIO[Option[AnalysisResult]] = {
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
  )(implicit currUsr: AuthenticatedUser): DBIO[Seq[Analysis]] = {
    val query = eventTable.filter { a =>
      a.partOf === parentId && a.museumId === mid
    } joinLeft resultTable on (_.eventId === _.eventId)

    query.result.map { res =>
      res.map { row =>
        toAnalysis(
          maybeEventId = row._1._1,
          maybeDoneDate = row._1._7,
          maybeAffectedThing = row._1._10.flatMap(ObjectUUID.fromString),
          rowAsJson = row._1._14
        ).flatMap(_.withResultAsOpt[Analysis](fromResultRowOpt(row._2))).get
      }
    }
  }

  private def sampleIdsForObjectAction(
      mid: MuseumId,
      oid: ObjectUUID
  )(implicit currUsr: AuthenticatedUser): DBIO[Seq[ObjectUUID]] = {
    sampleObjTable
      .filter(s => s.originatedFrom === oid && s.museumId === mid)
      .map(_.id)
      .result
  }

  private def listAllForObjectAction(
      mid: MuseumId,
      oid: ObjectUUID,
      sampleIds: Seq[ObjectUUID]
  )(implicit currUsr: AuthenticatedUser): DBIO[Seq[AnalysisModuleEvent]] = {
    val setid = SampleCreated.sampleEventTypeId

    def checkObjectUuid(id: Rep[Option[String]]) = {
      if (sampleIds.nonEmpty) id === oid.asString || (id inSet sampleIds.map(_.asString))
      else id === oid.asString
    }

    val qry = for {
      a <- eventTable.filter(a => a.museumId === mid && checkObjectUuid(a.affectedUuid))
      b <- eventTable
      if a.partOf === b.eventId || (b.eventId === a.eventId && a.eventTypeId === setid)
    } yield b

    val query = qry joinLeft resultTable on (_.eventId === _.eventId)

    query.result.map { res =>
      res.flatMap { row =>
        fromRow(
          maybeEventId = row._1._1,
          maybeDoneDate = row._1._7,
          maybeAffectedThing = row._1._10.flatMap(ObjectUUID.fromString),
          rowAsJson = row._1._14
        ).map {
          case ae: AnalysisEvent =>
            ae.withResultAsOpt(fromResultRowOpt(row._2)).getOrElse(ae)

          case so: SampleCreated =>
            so
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
  def insert(
      mid: MuseumId,
      a: Analysis
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[EventId]] = {
    insertEventWithAdditional(mid, a)(asRow) { (event, eid) =>
      event.result.map(res => upsertResultAction(mid, eid, res)).getOrElse(noaction)
    }
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
  def insertCol(
      mid: MuseumId,
      ac: AnalysisCollection
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[EventId]] = {
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
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[AnalysisEvent]]] = {
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
  )(
      implicit currUsr: AuthenticatedUser
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
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[AnalysisEvent]]] = {
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
  def listChildren(
      mid: MuseumId,
      id: EventId
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Seq[Analysis]]] = {
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
  )(
      implicit currUsr: AuthenticatedUser
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
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[AnalysisResult]]] = {
    val q = resultTable
      .filter(r => r.eventId === id && r.museumId === mid)
      .result
      .headOption
      .map(fromResultRowOpt)

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
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[EventId]] = {
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
   * Adds or updates a list of tupled EventId and AnalysisResult. The result
   * is added to the EventId in the same tuple instance.
   *
   * @param mid     the MuseumId
   * @param results the list of tupled EventId and AnalysisResults to add
   * @return eventually returns a MusitResult[Unit]
   */
  def upsertResults(
      mid: MuseumId,
      results: Seq[(EventId, AnalysisResult)]
  ): Future[MusitResult[Unit]] = {
    val actions = DBIO.sequence(results.map(er => upsertResultAction(mid, er._1, er._2)))

    db.run(actions.transactionally)
      .map(_ => MusitSuccess(()))
      .recover(
        nonFatal(
          s"An unexpected error occurred batch inserting a results to " +
            s"analyses: ${results.map(_._1).mkString(", ")}"
        )
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
  ): Future[MusitResult[Seq[AnalysisCollection]]] = {

    type AEQuery = Query[AnalysisEventTable, AnalysisEventTable#TableElementType, Seq]

    def buildQuery(ids: Seq[EventId]): AEQuery = eventTable.filter(_.eventId inSet ids)

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
                ON ee.AFFECTED_UUID = so.SAMPLE_UUID
              LEFT OUTER JOIN MUSIT_MAPPING.MUSITTHING mt
                ON ee.AFFECTED_UUID = mt.MUSITTHING_UUID
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
          val query = eids.foldLeft[(Int, AEQuery)]((0, eventTable)) {
            case (qry, ids) =>
              if (qry._1 == 0) (1, buildQuery(ids))
              else (qry._1 + 1, qry._2 unionAll buildQuery(ids))
          }
          db.run(query._2.result)
        } else {
          Future.successful(Vector.empty)
        }
      }
      .map(
        _.flatMap { row =>
          toAnalysisCollection(
            maybeEventId = row._1,
            maybeDoneDate = row._7,
            maybeAffectedThing = row._10.flatMap(ObjectUUID.fromString),
            rowAsJson = row._14
          )
        }
      )
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"An unexpected error occurred while fetching analysis events"))
  }

}
