package dao

import com.google.inject.Inject
import models.{ MuseumId, ObjectAggregation, ObjectId }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.driver.JdbcProfile

import scala.concurrent.Future

class ObjectAggregationDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] {

  def getOjects(museumId: Long): Future[Seq[ObjectAggregation]] = {
    Future.successful(Seq(
      ObjectAggregation(ObjectId(1), "Test", MuseumId(1))
    ))
  }

}
