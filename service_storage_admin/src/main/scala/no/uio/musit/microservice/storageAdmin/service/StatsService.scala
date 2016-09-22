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

class StatsService @Inject() (
    storageNodeService: StorageUnitService,
    statsDao: StatsDao
) extends Object {

  /**
   * Note that all methods here are counting total number of objects/nodes, irrespective of what the user actually
   * has access rights to see! (Most likely the user which requests this statistics will have access rights to
   * all nodes in the relevant subtree
   */

  /**
   * Number of museum object currently *directly* located at the storageNode with id = nodeId.
   * First verifies that nodeId actually exists, else a Bad Request will be returned
   */
  def museumObjectCount(nodeId: Long): MusitFuture[Int] = {
    storageNodeService.verifyStorageNodeExists(nodeId).musitFutureFlatMap { _ => directMuseumObjectCount(nodeId).toMusitFuture }
  }

  def subNodeCount(nodeId: Long): MusitFuture[Int] = {
    storageNodeService.verifyStorageNodeExists(nodeId).musitFutureFlatMap { _ =>
      directSubNodeCount(nodeId).toMusitFuture
    }
  }

  def recursiveMuseumObjectCount(nodeId: Long): MusitFuture[Int] = {
    storageNodeService.getStorageNodeDtoById(nodeId).musitFutureFlatMap {
      node => directRecursiveMuseumObjectCount(node).toMusitFuture
    }
  }

  def getStats(nodeId: Long): MusitFuture[Stats] = {
    storageNodeService.getStorageNodeDtoById(nodeId).musitFutureFlatMap {
      node =>
        (for {
          totalCount <- directRecursiveMuseumObjectCount(node)
          localCount <- directMuseumObjectCount(nodeId)
          subNodeCount <- directSubNodeCount(nodeId)
        } yield Stats(
          nodes = subNodeCount,
          objects = localCount,
          totalObjects = totalCount
        )).toMusitFuture
    }
  }

  /**
   * Same as museumObjectCount, but no verification that nodeId is an existing node. It assumes the node exists,
   * so if called on a non-existing node, 0 will be returned
   */
  private def directMuseumObjectCount(nodeId: Long): Future[Int] = {
    statsDao.getMuseumObjectCount(nodeId)
  }

  /** Same as subNodeCount, but no verification that nodeId is an existing node */
  private def directSubNodeCount(nodeId: Long): Future[Int] = {
    statsDao.getAllChildCount(nodeId)
  }

  /** Same as subNodeCount, but no verification that nodeId is an existing node */
  private def directRecursiveMuseumObjectCount(node: StorageNodeDTO): Future[Int] = {
    statsDao.getRecursiveMuseumObjectCount(node)
  }

}