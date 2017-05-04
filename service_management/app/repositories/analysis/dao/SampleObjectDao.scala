package repositories.analysis.dao

import com.google.inject.{Inject, Singleton}
import models.analysis.{SampleObject, Treatment}
import models.analysis.events.SampleCreated
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import no.uio.musit.models.{EventId, MuseumId, ObjectUUID}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class SampleObjectDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends AnalysisTables {

  val logger = Logger(classOf[SampleObjectDao])

  import profile.api._

  def insert(so: SampleObject): Future[MusitResult[ObjectUUID]] = {
    val soTuple = asSampleObjectTuple(so)
    val action  = sampleObjTable += soTuple

    db.run(action.transactionally).map(_ => MusitSuccess(soTuple._1)).recover {
      case NonFatal(ex) =>
        val msg = s"An unexpected error occurred inserting a sample object"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

  private def insertAnalysisAction(event: EventRow): DBIO[EventId] = {
    analysisTable returning analysisTable.map(_.id) += event
  }

  def insert(
      so: SampleObject,
      eventObj: SampleCreated
  ): Future[MusitResult[ObjectUUID]] = {
    val soTuple = asSampleObjectTuple(so)

    val action = for {
      _ <- sampleObjTable += soTuple
      _ <- insertAnalysisAction(asEventTuple(eventObj))
    } yield soTuple._1

    db.run(action.transactionally).map(MusitSuccess.apply).recover {
      case NonFatal(ex) =>
        val msg = s"An unexpected error occurred inserting a sample object"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

  def update(so: SampleObject): Future[MusitResult[Unit]] = {
    val a = sampleObjTable.filter(_.id === so.objectId).update(asSampleObjectTuple(so))

    db.run(a.transactionally).map {
      case res: Int if res == 1 => MusitSuccess(())
      case res: Int if 1 > res  => MusitDbError("Nothing was updated")
      case res: Int if 1 < res  => MusitDbError(s"Too many rows were updated: $res")
    }
  }

  def findByUUID(uuid: ObjectUUID): Future[MusitResult[Option[SampleObject]]] = {
    val q = sampleObjTable.filter(_.id === uuid).result.headOption

    db.run(q).map(sor => MusitSuccess(sor.map(fromSampleObjectRow))).recover {
      case NonFatal(ex) =>
        val msg = s"An unexpected error occurred fetching sample object $uuid"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

  def listForParentObject(parent: ObjectUUID): Future[MusitResult[Seq[SampleObject]]] = {
    val q = sampleObjTable.filter(_.parentId === parent).result

    db.run(q).map(_.map(fromSampleObjectRow)).map(MusitSuccess.apply).recover {
      case NonFatal(ex) =>
        val msg = s"An unexpected error occurred fetching child samples for $parent"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

  def listForMuseum(mid: MuseumId): Future[MusitResult[Seq[SampleObject]]] = {
    val q = sampleObjTable.filter(_.museumId === mid).result

    db.run(q).map(_.map(fromSampleObjectRow)).map(MusitSuccess.apply).recover {
      case NonFatal(ex) =>
        val msg = s"An unexpected error occurred fetching samples for Museum $mid"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

}
