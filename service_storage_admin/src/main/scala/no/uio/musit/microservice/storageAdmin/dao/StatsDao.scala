package no.uio.musit.microservice.storageAdmin.dao

import com.google.inject.{Inject, Singleton}
import no.uio.musit.microservice.storageAdmin.domain.dto.StorageNodeDTO
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile

import scala.concurrent.Future

@Singleton
class StatsDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    val storageNodeDao: StorageUnitDao
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  /*A query returning *all* non deleted child nodes, ignoring access rights. */
  private def getAllNonDeletedChildrenQuery(id: Long) = {
    storageNodeDao.StorageNodeTable.filter(st => st.isPartOf === id && st.isDeleted === false)
  }

  /** Count of *all* children of this node, irrespective of access rights to the children */
  def getChildrenCount(id: Long): Future[Int] = {
    db.run((getAllNonDeletedChildrenQuery(id).length).result)
  }

  /** The total number of museum objects at node or any of its subnodes */
  def getTotalMuseumObjectCount(node: StorageNodeDTO): Future[Int] = {
    val nodeFilter = node.nodePath.descendantsFilter
    db.run(sql"""
            SELECT count(*) FROM
            MUSARK_STORAGE.STORAGE_NODE n, MUSARK_STORAGE.LOCAL_OBJECT o
            WHERE n.NODE_PATH LIKE $nodeFilter and o.current_location_id = n.storage_node_id""".as[Int].head)
  }

  /** The number of museum objects directly at node only. (Not recursively including the objects at subnodes) */
  def getMuseumObjectCount(nodeId: Long): Future[Int] = {
    db.run(sql"""
            SELECT count(*) FROM
            MUSARK_STORAGE.LOCAL_OBJECT o
            WHERE o.current_location_id = $nodeId""".as[Int].head)
  }

  /** Only meant for tests! And only until we have merged the two microservices! */
  def testonlyInsertMuseumObjectAtNode(museumObjectId: Long, nodeId: Long) = {
    db.run(sqlu"""
            INSERT INTO MUSARK_STORAGE.LOCAL_OBJECT(object_id,current_location_id) values($museumObjectId, $nodeId)""")
  }

}