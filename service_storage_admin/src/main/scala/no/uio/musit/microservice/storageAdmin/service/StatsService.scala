package no.uio.musit.microservice.storageAdmin.service

/**
 * Created by jarle on 13.09.16.
 */

import com.google.inject.Inject
import no.uio.musit.microservice.storageAdmin.dao._
import no.uio.musit.microservice.storageAdmin.domain.Stats
import no.uio.musit.microservice.storageAdmin.domain.dto.StorageNodeDTO
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * A storage node id that we already have verified that exists in the database.
 * This is used to differentiate between methods which take as input
 * a nodeId which may or may not exist in the database, vs those methods in which the flow guarantees we already have
 * verified the existence of this storage node in the database.
 */
case class VerifiedNodeId(nodeId: Long) extends AnyVal

class StatsService @Inject() (
    storageNodeDao: StorageUnitDao,
    statsDao: StatsDao
) extends Object {

  /**
   * Note that all methods here are counting all objects/nodes in a given node, irrespective of what the user actually
   * has access rights to see! (Most likely the user which requests this statistics will have access rights to
   * all nodes in the relevant subtree, but that is not guaranteed.)
   * This is per spec. (Else the stats would lie). The check for whether a node is empty or not also need this behaviour.
   */

  private def getStorageNodeDtoById(nodeId: Long): MusitFuture[StorageNodeDTO] = {
    storageNodeDao.getStorageNodeDtoByIdAsMusitFuture(nodeId)
  }

  /**
   * Number of museum object currently *directly* located at the storageNode with id = nodeId.
   * First verifies that nodeId actually exists, else a Bad Request will be returned
   */
  def museumObjectCount(nodeId: Long): MusitFuture[Int] = {
    storageNodeDao.verifyStorageNodeExists(nodeId).musitFutureFlatMap { _ => museumObjectCount(VerifiedNodeId(nodeId)).toMusitFuture }
  }

  def subNodeCount(nodeId: Long): MusitFuture[Int] = {
    storageNodeDao.verifyStorageNodeExists(nodeId).musitFutureFlatMap { _ =>
      subNodeCount(VerifiedNodeId(nodeId)).toMusitFuture
    }
  }

  def totalMuseumObjectCount(nodeId: Long): MusitFuture[Int] = {
    getStorageNodeDtoById(nodeId).musitFutureFlatMap {
      node => totalMuseumObjectCount(node).toMusitFuture
    }
  }

  /**
   * Same as museumObjectCount, but no verification that nodeId is an existing node. It assumes the node exists.
   * If called with a non-existing node, at the moment 0 will be returned, but it would be proper for it to fire a require-failure then.
   */
  private def museumObjectCount(verifiedNodeId: VerifiedNodeId): Future[Int] = {
    statsDao.getMuseumObjectCount(verifiedNodeId.nodeId)
  }

  /** Same as subNodeCount, but no verification that nodeId is an existing node, it assumes this has already been done. */
  private def subNodeCount(verifiedNodeId: VerifiedNodeId): Future[Int] = {
    statsDao.getChildrenCount(verifiedNodeId.nodeId)
  }

  /** Same as subNodeCount, but no verification that nodeId is an existing node, it assumes this has already been done. */
  private def totalMuseumObjectCount(node: StorageNodeDTO): Future[Int] = {
    statsDao.getTotalMuseumObjectCount(node)
  }

  def getStats(nodeId: Long): MusitFuture[Stats] = {
    getStorageNodeDtoById(nodeId).musitFutureFlatMap {
      node =>
        val verifiedNodeId = VerifiedNodeId(nodeId)
        (for {
          totalObjectCount <- totalMuseumObjectCount(node)
          objectCount <- museumObjectCount(verifiedNodeId)
          nodeCount <- subNodeCount(verifiedNodeId)
        } yield Stats(
          nodes = nodeCount,
          objects = objectCount,
          totalObjects = totalObjectCount
        )).toMusitFuture
    }
  }

  def nodeIsEmpty(nodeId: Long): MusitFuture[Boolean] = {
    getStorageNodeDtoById(nodeId).musitFutureFlatMap {
      node =>
        val verifiedNodeId = VerifiedNodeId(nodeId)
        (for {
          objectCount <- museumObjectCount(verifiedNodeId)
          nodeCount <- subNodeCount(verifiedNodeId)
        } yield (objectCount + nodeCount) == 0)
          .toMusitFuture
    }
  }
}