package repositories.analysis.dao

import com.google.inject.{Inject, Singleton}
import models.analysis.events.AnalysisResults.AnalysisResult
import models.analysis.events.{Analysis, AnalysisCollection, AnalysisEvent}
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import no.uio.musit.models.{EventId, ObjectUUID}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class AnalysisDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends Tables {

  val logger = Logger(classOf[AnalysisDao])

  import driver.api._

  private val noaction: DBIO[Unit] = DBIO.successful(())

  private def insertAnalysisAction(event: EventRow): DBIO[EventId] = {
    analysisTable returning analysisTable.map(_.id) += event
  }

  private def insertResultAction(result: ResultRow): DBIO[Long] = {
    resultTable returning resultTable.map(_.id) += result
  }

  private def insertAnalysisWithResultAction(a: Analysis): DBIO[EventId] = {
    for {
      id <- insertAnalysisAction(asEventTuple(a))
      _ <- a.result.map(r => insertResultAction(asResultTuple(id, r))).getOrElse(noaction)
    } yield id
  }

  private def insertChildEventsAction(
    pid: EventId,
    events: Seq[Analysis]
  ): DBIO[Seq[EventId]] = {
    val batch = events.map { e =>
      insertAnalysisWithResultAction(e.copy(partOf = Some(pid)))
    }
    DBIO.sequence(batch)
  }

  private def findByIdAction(id: EventId): DBIO[Option[AnalysisEvent]] = {
    analysisTable.filter(_.id === id).result.headOption.map { res =>
      res.flatMap(fromEventRow)
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
        fromEventRow(row._1)
          .flatMap(_.withResultAsOpt(fromResultRow(row._2))).get
      }
    }
  }

  private def listForObjectAction(oid: ObjectUUID): DBIO[Seq[AnalysisEvent]] = {
    val query = analysisTable.filter(_.objectUuid === oid) joinLeft
      resultTable on (_.id === _.eventId)

    query.result.map { res =>
      res.flatMap { row =>
        fromEventRow(row._1).map { ue =>
          ue.withResultAsOpt(fromResultRow(row._2)).getOrElse(ue)
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
    val action = insertAnalysisWithResultAction(a)

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
      id <- insertAnalysisAction(asEventTuple(ac.withoutChildren))
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
   * Locates a specific Analysis event by its EventId.
   *
   * @param id the EventId to look for.
   * @return eventually returns a MusitResult that might contain the AnalysisEvent.
   */
  def findById(id: EventId): Future[MusitResult[Option[AnalysisEvent]]] = {
    val query = for {
      maybeEvent <- findByIdAction(id)
      maybeRes <- resultForEventIdAction(id)
      children <- listChildrenAction(id)
    } yield {
      maybeEvent.map {
        case a: Analysis => a.withResult(maybeRes)
        case ac: AnalysisCollection => ac.copy(events = children)
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
   * Locate all analysis' related to the provided ObjectUUID.
   *
   * @param oid The ObjectUUID to find analysis' for
   * @return eventually a result with a list of analysis events and their results
   */
  def findByObjectUUID(oid: ObjectUUID): Future[MusitResult[Seq[AnalysisEvent]]] = {
    db.run(listForObjectAction(oid)).map(MusitSuccess.apply).recover {
      case NonFatal(ex) =>
        val msg = s"An unexpected error occurred fetching events for object $oid"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

  /**
   * Adds a new result to the analysis with the given EventId.
   *
   * @param id The EventId of the analysis that has a new result.
   * @param res The AnalysisResult to add.
   * @return eventually a result with the database ID of the saved result.
   */
  def insertResult(id: EventId, res: AnalysisResult): Future[MusitResult[Long]] = {
    val action = insertResultAction(asResultTuple(id, res))

    db.run(action).map(MusitSuccess.apply).recover {
      case NonFatal(ex) =>
        val msg = s"An unexpected error occurred inserting a result to analysis $id"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

}
