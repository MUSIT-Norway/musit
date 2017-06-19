package repositories.reporting.dao

import com.google.inject.{Inject, Singleton}
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.models._
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.shared.dao.{DbErrorHandlers, StorageTables}

import scala.concurrent.Future

@Singleton
class StorageStatsDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends StorageTables
    with DbErrorHandlers {

  val logger = Logger(classOf[StorageStatsDao])

  import profile.api._

  private def countChildren(id: StorageNodeId): DBIO[Int] = {
    for {
      q1 <- storageNodeTable.filter(_.uuid === id).map(_.id).result.headOption
      q2 <- q1.map { pid =>
             storageNodeTable
               .filter(sn => sn.isPartOf === pid && sn.isDeleted === false)
               .length
               .result
           }.getOrElse(DBIO.successful(0))
    } yield q2
  }

  /**
   * Count of *all* children of this node, irrespective of access rights to
   * the children
   */
  def numChildren(id: StorageNodeId): Future[MusitResult[Int]] = {
    db.run(countChildren(id))
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"An error occurred counting number node children under $id"))
  }

  /**
   * The total number of museum objects at node or any of its child nodes.
   *
   * @param path NodePath to count total object count for.
   * @return Future[Int] with total number of objects under the provided node
   *         and all its child nodes.
   */
  def numObjectsInPath(path: NodePath): Future[MusitResult[Int]] = {
    val nodeFilter = s"${path.path}%"

    logger.debug(s"Using node filter: $nodeFilter")

    val query =
      sql"""
        SELECT /*+DRIVING_SITE(mt)*/ COUNT(1) FROM
          "MUSARK_STORAGE"."STORAGE_NODE" sn,
          "MUSARK_STORAGE"."NEW_LOCAL_OBJECT" lo,
          "MUSIT_MAPPING"."MUSITTHING" mt
        WHERE sn."NODE_PATH" LIKE '#${nodeFilter}'
        AND sn."STORAGE_NODE_UUID" = lo."CURRENT_LOCATION_ID"
        AND mt."IS_DELETED" = 0
        AND lo."OBJECT_UUID" = mt."MUSITTHING_UUID"
      """.as[Int].head

    db.run(query)
      .map { vi =>
        logger.debug(s"Num objects in path $path is $vi")
        MusitSuccess.apply(vi)
      }
      .recover(
        nonFatal(s"An error occurred counting total objects for nodes in path $path")
      )
  }

  /**
   * The number of museum objects directly at the given node.
   * To calculate the total number of objects for nodes in the tree,
   * use the {{{totalObjectCount}}} method.
   *
   * @param nodeId StorageNodeId to count objects for.
   * @return Future[Int] with the number of objects directly on the provided nodeId
   */
  def numObjectsInNode(nodeId: StorageNodeId): Future[MusitResult[Int]] = {
    val query = {
      val idAsString = nodeId.asString
      sql"""
        SELECT /*+DRIVING_SITE(mt)*/ COUNT(1) FROM
          "MUSIT_MAPPING"."MUSITTHING" mt,
          "MUSARK_STORAGE"."NEW_LOCAL_OBJECT" lo
        WHERE mt."IS_DELETED" = 0
        AND lo."CURRENT_LOCATION_ID" = ${idAsString}
        AND lo."OBJECT_UUID" = mt."MUSITTHING_UUID"
      """.as[Int].head
    }

    db.run(query)
      .map { vi =>
        logger.debug(s"Num objects in node $nodeId is $vi")
        MusitSuccess.apply(vi)
      }
      .recover(nonFatal(s"An error occurred counting number direct objects in $nodeId"))
  }
}
