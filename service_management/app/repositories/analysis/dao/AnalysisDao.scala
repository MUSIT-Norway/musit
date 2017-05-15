package repositories.analysis.dao

import com.google.inject.{Inject, Singleton}
import models.analysis.events.AnalysisResults.AnalysisResult
import models.analysis.events._
import no.uio.musit.MusitResults.{
  MusitDbError,
  MusitResult,
  MusitSuccess,
  MusitValidationError
}
import no.uio.musit.models.{EventId, ObjectUUID}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class AnalysisDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends AnalysisTables {

  val logger = Logger(classOf[AnalysisDao])

  import profile.api._

  private val noaction: DBIO[Unit] = DBIO.successful(())

  private def insertAnalysisAction(event: EventRow): DBIO[EventId] = {
    analysisTable returning analysisTable.map(_.id) += event
  }

  private def upsertResultAction(id: EventId, result: AnalysisResult): DBIO[Int] = {
    resultTable.insertOrUpdate(asResultTuple(id, result))
  }

  private def insertAnalysisWithResultAction(
      ae: AnalysisEvent,
      maybeRes: Option[AnalysisResult]
  ): DBIO[EventId] = {
    for {
      id <- insertAnalysisAction(asEventTuple(ae))
      _  <- maybeRes.map(r => upsertResultAction(id, r)).getOrElse(noaction)
    } yield id
  }

  private def insertChildEventsAction(
      pid: EventId,
      events: Seq[Analysis]
  ): DBIO[Seq[EventId]] = {
    val batch = events.map { e =>
      insertAnalysisWithResultAction(e.copy(partOf = Some(pid)), e.result)
    }
    DBIO.sequence(batch)
  }

  private def updateAction(
      id: EventId,
      event: AnalysisEvent
  ): DBIO[Int] = {
    analysisTable.filter(_.id === id).update(asEventTuple(event))
  }

  private def findByIdAction(
      id: EventId,
      includeSample: Boolean
  ): DBIO[Option[AnalysisModuleEvent]] = {
    val q1 = analysisTable.filter(_.id === id)
    val q2 = {
      if (includeSample) q1
      else q1.filter(_.typeId =!= SampleCreated.sampleEventTypeId)
    }

    q2.result.headOption.map { res =>
      res.flatMap(toAnalysisModuleEvent)
    }
  }

  private def resultForEventIdAction(id: EventId): DBIO[Option[AnalysisResult]] = {
    resultTable.filter(_.eventId === id).result.headOption.map { res =>
      res.flatMap(fromResultRow)
    }
  }

  private def listChildrenAction(parentId: EventId): DBIO[Seq[Analysis]] = {
    val query = analysisTable.filter(_.partOf === parentId) joinLeft
      resultTable on (_.id === _.eventId)

    query.result.map { res =>
      res.map { row =>
        toAnalysis(row._1)
          .flatMap(_.withResultAsOpt[Analysis](fromResultRow(row._2)))
          .get
      }
    }
  }

  private def listForObjectAction(oid: ObjectUUID): DBIO[Seq[AnalysisModuleEvent]] = {
    val query = analysisTable.filter(_.objectUuid === oid) joinLeft
      resultTable on (_.id === _.eventId)

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
   * @param a The Analysis to persist.
   * @return eventually returns a MusitResult containing the EventId.
   */
  def insert(a: Analysis): Future[MusitResult[EventId]] = {
    val action = insertAnalysisWithResultAction(a, a.result)

    db.run(action.transactionally).map(MusitSuccess.apply).recover {
      case NonFatal(ex) =>
        val msg = s"An unexpected error occurred inserting an analysis event"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

  /**
   * Inserts an {{{AnalysisCollection}}} to the DB. The difference from inserting
   * a bunch of analysis' one by one is that; each Analysis row will get their,
   * partOf attribute set to the EventId of the bounding {{{AnalysisCollection}}}.
   *
   * @param ac The AnalysisCollection to persist.
   * @return eventually returns a MusitResult containing the EventId.
   */
  def insertCol(ac: AnalysisCollection): Future[MusitResult[EventId]] = {
    val action = for {
      id   <- insertAnalysisWithResultAction(ac.withoutChildren, ac.result)
      eids <- insertChildEventsAction(id, ac.events)
    } yield id

    db.run(action.transactionally).map(MusitSuccess.apply).recover {
      case NonFatal(ex) =>
        val msg = s"An unexpected error occurred inserting an analysis event collection"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

  /**
   * Performs an update action against the DB using the values in the provided
   * {{{AnalysisEvent}}} argument.
   *
   * @param id the EventId associated with the analysis event to update
   * @param ae the AnalysisEvent to update
   * @return a result with an option of the updated event
   */
  def update(
      id: EventId,
      ae: AnalysisEvent
  ): Future[MusitResult[Option[AnalysisEvent]]] = {
    val action = updateAction(id, ae).transactionally

    db.run(action)
      .flatMap { numUpdated =>
        if (numUpdated == 1) {
          findAnalysisById(id)
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
      .recover {
        case NonFatal(ex) =>
          val msg = s"An unexpected error occurred inserting an analysis event"
          logger.error(msg, ex)
          MusitDbError(msg, Option(ex))
      }
  }

  /**
   * Locates a specific analysis module related event by its EventId.
   *
   * @param id the EventId to look for.
   * @param includeSample Boolean flag to control which event types are returned.
   * @return eventually returns a MusitResult that might contain the AnalysisModuleEvent.
   */
  def findById(
      id: EventId,
      includeSample: Boolean = true
  ): Future[MusitResult[Option[AnalysisModuleEvent]]] = {
    val query = for {
      maybeEvent <- findByIdAction(id, includeSample)
      maybeRes   <- resultForEventIdAction(id)
      children   <- listChildrenAction(id)
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

    db.run(query).map(MusitSuccess.apply).recover {
      case NonFatal(ex) =>
        val msg = s"An unexpected error occurred fetching event $id"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

  /**
   * Same as findById, but will ensure that only the Analysis specific events
   * are returned.
   *
   * @param id The event ID to look for
   * @return the AnalysisEvent that was found or None
   */
  def findAnalysisById(id: EventId): Future[MusitResult[Option[AnalysisEvent]]] = {
    findById(id, includeSample = false).map(_.map(_.flatMap {
      case ae: AnalysisEvent => Some(ae)
      case _                 => None
    }))
  }

  /**
   * Find all analysis events that are _part of_ an analysis container.
   *
   * Children can _only_ be of type {{{Analysis}}}.
   *
   * @param id The analysis container to find children for.
   * @return eventually a result with a list of analysis events and their results
   */
  def listChildren(id: EventId): Future[MusitResult[Seq[Analysis]]] = {
    db.run(listChildrenAction(id)).map(MusitSuccess.apply).recover {
      case NonFatal(ex) =>
        val msg = s"An unexpected error occurred fetching events partOf $id"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

  /**
   * Locate all events related to the provided ObjectUUID.
   *
   * @param oid The ObjectUUID to find analysis' for
   * @return eventually a result with a list of analysis events and their results
   */
  def findByObjectUUID(oid: ObjectUUID): Future[MusitResult[Seq[AnalysisModuleEvent]]] = {
    db.run(listForObjectAction(oid)).map(MusitSuccess.apply).recover {
      case NonFatal(ex) =>
        val msg = s"An unexpected error occurred fetching events for object $oid"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

  /**
   * Usefull method for locating the result for a specific analysis event.
   *
   * @param id the EventId of the analysis event to fetch the result for
   * @return a result that may or may not contain the AnalysisResult
   */
  def findResultFor(id: EventId): Future[MusitResult[Option[AnalysisResult]]] = {
    val q = resultTable.filter(_.eventId === id).result.headOption.map(fromResultRow)

    db.run(q).map(MusitSuccess.apply).recover {
      case NonFatal(ex) =>
        val msg = s"An unexpected error occurred looking up result for analysis $id"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

  /**
   * Adds or updates a result for the analysis with the given EventId.
   *
   * @param id  The EventId of the analysis that has a new result.
   * @param res The AnalysisResult to add.
   * @return eventually a result with the EventId the saved result belongs to.
   */
  def upsertResult(id: EventId, res: AnalysisResult): Future[MusitResult[EventId]] = {
    val action = upsertResultAction(id, res)

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
      .recover {
        case NonFatal(ex) =>
          val msg = s"An unexpected error occurred inserting a result to analysis $id"
          logger.error(msg, ex)
          MusitDbError(msg, Option(ex))
      }
  }

}
