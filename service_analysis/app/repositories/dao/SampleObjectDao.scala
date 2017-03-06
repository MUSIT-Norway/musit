package repositories.dao

import com.google.inject.{Inject, Singleton}
import models.SampleObject
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import no.uio.musit.models.ObjectUUID
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class SampleObjectDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends Tables {

  val logger = Logger(classOf[SampleObjectDao])

  import driver.api._

  def insert(so: SampleObject): Future[MusitResult[ObjectUUID]] = {
    val soTuple = asSampleObjectTuple(so)
    val action = sampleObjTable += soTuple

    db.run(action.transactionally).map(_ => MusitSuccess(soTuple._1)).recover {
      case NonFatal(ex) =>
        val msg = s"An unexpected error occurred inserting a sample object"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

  def update(so: SampleObject): Future[MusitResult[Int]] = {
    val a = sampleObjTable.filter(_.id === so.objectId).update(asSampleObjectTuple(so))

    db.run(a.transactionally).map {
      case res: Int if res == 1 => MusitSuccess(res)
      case res: Int if 1 > res => MusitDbError("Nothing was updated")
      case res: Int if 1 < res => MusitDbError(s"Too many rows were updated: $res")
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

}
