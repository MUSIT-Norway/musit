package dao

import com.google.inject.Inject
import models.{ NodeId, ObjectAggregation, ObjectId }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.driver.JdbcProfile

import scala.concurrent.Future

class ObjectAggregationDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] {

  def getOjects(nodeId: Long): Future[Seq[ObjectAggregation]] = {
    Future.successful(Seq(
      ObjectAggregation(ObjectId(1), "Test", NodeId(1))
    ))
  }

}
