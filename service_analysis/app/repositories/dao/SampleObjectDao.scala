package repositories.dao

import com.google.inject.{Inject, Singleton}
import models.SampleObject
import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.models.{ObjectId, ObjectUUID}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future

@Singleton
class SampleObjectDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends Tables {

  val logger = Logger(classOf[SampleObjectDao])

  import driver.api._

  def insert(so: SampleObject): Future[MusitResult[ObjectId]] = {

    ???
  }

  def update(so: SampleObject): Future[MusitResult[Int]] = {
    ???
  }

  def findById(id: ObjectId): Future[MusitResult[Option[SampleObject]]] = {
    ???
  }

  def findByUUID(uuid: ObjectUUID): Future[MusitResult[Option[SampleObject]]] = {
    ???
  }

  def listForParentObject(parent: ObjectUUID): Future[MusitResult[Seq[SampleObject]]] = {
    ???
  }

}
