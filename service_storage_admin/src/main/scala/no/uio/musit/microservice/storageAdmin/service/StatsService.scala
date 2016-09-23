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
   * Note that all methods here are counting all objects/nodes in a given node, irrespective of what the user actually
   * has access rights to see! (Most likely the user which requests this statistics will have access rights to
   * all nodes in the relevant subtree, but that is not guaranteed.)
   * This is per spec. (Else the stats would lie). The check for whether a node is empty or not also need this behaviour.
   */

  /**
   * Number of museum object currently *directly* located at the storageNode with id = nodeId.
   * First verifies that nodeId actually exists, else a Bad Request will be returned
   */
  def museumObjectCount(nodeId: Long): MusitFuture[Int] = {
    storageNodeService.verifyStorageNodeExists(nodeId).musitFutureFlatMap { _ => museumObjectCount_NodeVerified(nodeId).toMusitFuture }
  }

  def subNodeCount(nodeId: Long): MusitFuture[Int] = {
    storageNodeService.verifyStorageNodeExists(nodeId).musitFutureFlatMap { _ =>
      subNodeCount_NodeVerified(nodeId).toMusitFuture
    }
  }

  def totalMuseumObjectCount(nodeId: Long): MusitFuture[Int] = {
    storageNodeService.getStorageNodeDtoById(nodeId).musitFutureFlatMap {
      node => totalMuseumObjectCount_NodeVerified(node).toMusitFuture
    }
  }

  def getStats(nodeId: Long): MusitFuture[Stats] = {
    storageNodeService.getStorageNodeDtoById(nodeId).musitFutureFlatMap {
      node =>
        (for {
          totalObjectCount <- totalMuseumObjectCount_NodeVerified(node)
          objectCount <- museumObjectCount_NodeVerified(nodeId)
          nodeCount <- subNodeCount_NodeVerified(nodeId)
        } yield Stats(
          nodes = nodeCount,
          objects = objectCount,
          totalObjects = totalObjectCount
        )).toMusitFuture
    }
  }

  /**
   * Same as museumObjectCount, but no verification that nodeId is an existing node. It assumes the node exists,
   * so if called on a non-existing node, 0 will be returned
   */
  private def museumObjectCount_NodeVerified(nodeId: Long): Future[Int] = {
    statsDao.getMuseumObjectCount(nodeId)
  }

  /** Same as subNodeCount, but no verification that nodeId is an existing node */
  private def subNodeCount_NodeVerified(nodeId: Long): Future[Int] = {
    statsDao.getChildrenCount(nodeId)
  }

  /** Same as subNodeCount, but no verification that nodeId is an existing node */
  private def totalMuseumObjectCount_NodeVerified(node: StorageNodeDTO): Future[Int] = {
    statsDao.getTotalMuseumObjectCount(node)
  }

}