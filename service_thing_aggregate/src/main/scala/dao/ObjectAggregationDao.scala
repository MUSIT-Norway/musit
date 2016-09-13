package dao

import com.google.inject.Inject
import models.{ NodeId, ObjectAggregation, ObjectId }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.driver.JdbcProfile

import scala.concurrent.Future

class ObjectAggregationDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] {

  def getObjects(nodeId: Long): Future[Seq[ObjectAggregation]] = {
    Future.successful(Seq(
      ObjectAggregation(ObjectId(1), "øks", "C666", Some("1a"), NodeId(1))
    ))
  }


  /* sql: select */
  /* sql-spørring som gir musitThing-objekter til en gitt node
 select object_id,displayName,displayId from MUSIT_MAPPING.VIEW_MUSITTHING VM,MUSARK_STORAGE.LOCAL_OBJECT L
  WHERE L.current_location_id = NODE_ID and l.object_id = vm.ny_id*/
}
