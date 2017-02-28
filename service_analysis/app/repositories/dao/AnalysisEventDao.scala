package repositories.dao

import com.google.inject.{Inject, Singleton}
import models.events.AnalysisEvent
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import no.uio.musit.models.EventId
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json._

import scala.concurrent.Future
import scala.util.control.NonFatal

import play.api.libs.concurrent.Execution.Implicits.defaultContext

@Singleton
class AnalysisEventDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends Tables {

  val logger = Logger(classOf[AnalysisEventDao])

  import driver.api._

  def insert[A <: AnalysisEvent](
    event: A
  )(implicit w: Writes[A]): Future[MusitResult[EventId]] = {
    val action = eventTable returning eventTable.map(_.id) += asTuple(event)

    db.run(action).map(MusitSuccess.apply).recover {
      case NonFatal(ex) =>
        val msg = s"An unexpected error occurred inserting an analysis event"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

  def findById(id: EventId): Future[MusitResult[Option[AnalysisEvent]]] = {
    val query = eventTable.filter(_.id === id)
    db.run(query.result.headOption).map { res =>
      MusitSuccess(res.flatMap(asEvent))
    }.recover {
      case NonFatal(ex) =>
        val msg = s"An unexpected error occurred fetching event $id"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

}
