/*
 * MUSIT is a museum database to archive natural and cultural history data.
 * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package services

import java.util.NoSuchElementException

import models.event.envreq.EnvRequirement
import models.event.move._
import models.storage.StorageType._
import models.storage._
import no.uio.musit.MusitResults._
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models._
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.dao.storage.StorageUnitDao

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.control.NonFatal

trait NodeService {

  private val logger = Logger(classOf[NodeService])

  val unitDao: StorageUnitDao
  val envReqService: EnvironmentRequirementService

  /**
   * Saves the provided environment requirements as an Event.
   */
  private[services] def saveEnvReq(
    mid: MuseumId,
    nodeId: StorageNodeDatabaseId,
    envReq: EnvironmentRequirement
  )(implicit currUsr: AuthenticatedUser): Future[Option[EnvironmentRequirement]] = {
    unitDao.getById(mid, nodeId).flatMap { mayBeNode =>
      mayBeNode.map { _ =>
        val now = dateTimeNow
        val er = EnvRequirement.toEnvRequirementEvent(currUsr.id, nodeId, now, envReq)

        envReqService.add(mid, er).map {
          case MusitSuccess(success) =>
            logger.debug("Successfully wrote environment requirement data " +
              s"for node $nodeId")
            Some(EnvRequirement.fromEnvRequirementEvent(er))

          case err: MusitError =>
            logger.error("Something went wrong while storing the environment " +
              s"requirements for node $nodeId")
            None
        }
      }.getOrElse(Future.successful(None))
    }
  }

  /**
   * Find the NodePath for the given storageNodeId.
   */
  private[services] def findPath(
    mid: MuseumId,
    maybeId: Option[StorageNodeDatabaseId]
  ): Future[Option[NodePath]] = {
    maybeId.map(id => unitDao.getPathById(mid, id))
      .getOrElse(Future.successful(None))
  }

  /**
   * Helper method to find PathNames for a potentially present StorageNodeId.
   *
   * @param mid     MuseumId
   * @param maybeId Option[StorageNodeId]
   * @return Future[(NodePath, Seq[NamedPathElement])]
   */
  private[services] def findPathAndNames(
    mid: MuseumId,
    maybeId: Option[StorageNodeDatabaseId]
  ): Future[(NodePath, Seq[NamedPathElement])] = {
    findPath(mid, maybeId).flatMap { maybePath =>
      maybePath.map(p => unitDao.namesForPath(p).map(names => (p, names)))
        .getOrElse(Future.successful((NodePath.empty, Seq.empty)))
    }
  }

  /**
   * Helper method to check if a node is empty or not.
   *
   * @param node the StorageNode to check
   * @return a Future[Boolean] that is true if node is empty, else false
   */
  private[services] def isEmpty(node: StorageNode): Future[Boolean] = {
    node.id.map { nodeId =>
      val eventuallyTotal = MusitResultT(unitDao.numObjectsInNode(nodeId))
      val eventuallyNode = MusitResultT(unitDao.numChildren(nodeId))

      val emptyNode = for {
        total <- eventuallyTotal
        nodeCount <- eventuallyNode
      } yield (total + nodeCount) == 0

      emptyNode.value.map(_.toOption.getOrElse(false))

    }.getOrElse(Future.successful(false))
  }

  /**
   * This function helps to validate that the relevant rules about the node
   * hierarchy are enforced. All operations that involves positioning a node
   * somewhere in the hierarchy, will need to call this function to ensure
   * validity.
   *
   * @param mid  MuseumId
   * @param node an instance of T which must be a sub-type of StorageNode
   * @param dest the destination NodePath.
   * @tparam T the specific StorageNode type
   * @return a Future of Boolean indicating valid or invalid positioning
   */
  private[services] def isValidPosition[T <: StorageNode](
    mid: MuseumId,
    node: T,
    dest: NodePath
  ): Future[Boolean] = {
    if (!dest.childOf(node.path)) {
      val maybeDestId = dest.asIdSeq.lastOption
      // Get the StorageType for the elements in the destination path so we can
      // use it to verify that nodes are placed on a valid location
      unitDao.getStorageTypesInPath(mid, dest).map { idTypeTuples =>
        logger.debug(s"Found types for node IDs in destination path" +
          s": ${idTypeTuples.mkString(", ")}")
        logger.trace(s"Validating destination for ${node.storageType.entryName}...")
        // Identify the type of node we want to place in the hierarchy, and
        // validate if the destination location is valid for the given type.
        node.storageType match {
          case RootType =>
            RootNode.isValidLocation(dest)

          case RootLoanType =>
            RootNode.isValidLocation(dest)

          case OrganisationType =>
            Organisation.isValidLocation(maybeDestId, idTypeTuples)

          case BuildingType =>
            Building.isValidLocation(maybeDestId, idTypeTuples)

          case RoomType =>
            Room.isValidLocation(maybeDestId, idTypeTuples)

          case StorageUnitType =>
            StorageUnit.isValidLocation(maybeDestId, idTypeTuples)
        }
      }
    } else {
      logger.warn(s"destination $dest is not allowed because it's a child" +
        s" of ${node.path}")
      Future.successful(false)
    }
  }

  // A couple of type aliases to reduce the length of some function args.
  type NodeInsertIO[A] = (MuseumId, A) => Future[StorageNodeDatabaseId]
  type SetEnvReq[A] = (A, Option[EnvironmentRequirement]) => A
  type NodeUpdateIO[A] = (StorageNodeDatabaseId, NodePath) => Future[MusitResult[Unit]]
  type GetNodeIO[A] = (MuseumId, StorageNodeDatabaseId) => Future[MusitResult[Option[A]]]

  private val futureFilterErr = "Future.filter predicate is not satisfied"

  /**
   * Helper function that wraps the process of inserting a new StorageNode.
   *
   * @param node
   * @param insert
   * @param setEnvReq
   * @param updateWithPath
   * @tparam T
   * @return
   */
  private[services] def addNode[T <: StorageNode](
    mid: MuseumId,
    node: T,
    insert: NodeInsertIO[T],
    setEnvReq: SetEnvReq[T],
    updateWithPath: NodeUpdateIO[T],
    getNode: GetNodeIO[T]
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[T]]] = {
    val res = for {
      maybePath <- findPath(mid, node.isPartOf)
      isValidDest <- isValidPosition(mid, node, maybePath.getOrElse(NodePath.empty))
      // Call the insert function to persist the node if the path is valid.
      if isValidDest
      nodeId <- insert(mid, node)
      pathUpdated <- updateWithPath(nodeId, maybePath.getOrElse(NodePath.empty).appendChild(nodeId)) // scalastyle:ignore
      _ <- node.environmentRequirement.map(er => saveEnvReq(mid, nodeId, er)).getOrElse(Future.successful(None)) // scalastyle:ignore
      theNode <- getNode(mid, nodeId)
    } yield {
      logger.debug(s"Successfully added node ${node.name} of type" +
        s" ${node.storageType} to ${maybePath.getOrElse(NodePath.empty)}")
      theNode
    }

    res.recover {
      // TODO: Use custom defined exception in if guard in above for-comprehension.
      case nse: NoSuchElementException if nse.getMessage.contains(futureFilterErr) =>
        val msg = s"Invalid destination for ${node.name} of type ${node.storageType}"
        logger.warn(msg)
        MusitValidationError(msg)

      case NonFatal(ex) =>
        val msg = s"An unexpected error occurred trying to add node" +
          s" ${node.name} of type ${node.storageType}"
        logger.error(msg, ex)
        MusitInternalError(msg)
    }
  }

  private[services] def getEnvReq(
    mid: MuseumId,
    id: StorageNodeDatabaseId
  ): Future[Option[EnvironmentRequirement]] = {
    envReqService.findLatestForNodeId(mid, id).map {
      case MusitSuccess(maybeEnvRequirement) => maybeEnvRequirement
      case _ => None

    }.recover {
      case NonFatal(ex) =>
        // If we fail fetching the envreq event, we'll return None.
        logger.warn("Something went wrong trying to locate latest " +
          s"environment requirement data for unit $id", ex)
        None
    }
  }

  type CopyNode[A <: StorageNode] = (A, Option[EnvironmentRequirement], Option[Seq[NamedPathElement]]) => A // scalastyle:ignore

  /**
   * Helper function that applies the common logic for fetching a storage node.
   *
   * @param mid
   * @param id
   * @param eventuallyMaybeNode
   * @param cp
   * @tparam A
   * @return
   */
  private[services] def nodeById[A <: StorageNode](
    mid: MuseumId,
    id: StorageNodeDatabaseId,
    eventuallyMaybeNode: Future[Option[A]]
  )(cp: CopyNode[A]): Future[MusitResult[Option[A]]] = {
    val eventuallyMaybeEnvReq = getEnvReq(mid, id)
    for {
      maybeNode <- eventuallyMaybeNode
      maybeEnvReq <- eventuallyMaybeEnvReq
      namedPathElems <- maybeNode.map { node =>
        unitDao.namesForPath(node.path)
      }.getOrElse(Future.successful(Seq.empty))
    } yield {
      val maybePathElems = {
        if (namedPathElems.nonEmpty) Some(namedPathElems)
        else None
      }
      MusitSuccess(maybeNode.map(n => cp(n, maybeEnvReq, maybePathElems)))
    }
  }

  /**
   * Deals with the shared logic for registering a Move event.
   *
   * @param event
   * @param eventuallyMaybeCurrent
   * @param eventuallyMaybeTo
   * @param mv
   * @tparam E
   * @return
   */
  private[services] def move[E <: MoveEvent](
    event: E,
    eventuallyMaybeCurrent: Future[Option[GenericStorageNode]],
    eventuallyMaybeTo: Future[Option[GenericStorageNode]]
  )(
    mv: (Option[GenericStorageNode], GenericStorageNode) => Future[MusitResult[EventId]]
  ): Future[MusitResult[EventId]] = {
    val eventuallyExistence = for {
      maybeCurrent <- eventuallyMaybeCurrent
      maybeTo <- eventuallyMaybeTo
    } yield (maybeCurrent, maybeTo)

    eventuallyExistence.flatMap {
      case (maybeCurr: Option[GenericStorageNode], maybeTo: Option[GenericStorageNode]) =>
        if (maybeCurr.flatMap(_.id) != maybeTo.flatMap(_.id)) {
          maybeTo.map { to =>
            mv(maybeCurr, to)
          }.getOrElse {
            Future.successful(MusitValidationError("Could not find destination node."))
          }
        } else {
          Future.successful {
            MusitValidationError("Current node and destination are the same.")
          }
        }
    }
  }

  type CurrLocType[ID] = Map[ID, Option[StorageNodeDatabaseId]]

  private def filterAndEnrich[ID <: MusitId, E <: MoveEvent](
    current: CurrLocType[ID],
    ids: Vector[ID],
    moveEvents: Seq[E]
  )(implicit ctId: ClassTag[ID], ctEvt: ClassTag[E]): Seq[E] = {
    moveEvents.filter(_.affectedThing.exists(ids.contains)).map { e =>
      val currId = e.affectedThing.get.asInstanceOf[ID] // scalastyle:ignore
      val id = current.get(currId).flatten
      // need to match on type to be able to access the copy function.
      val copied = e match {
        case obj: MoveObject => obj.copy(from = id)
        case nde: MoveNode => nde.copy(from = id)
      }
      // Don't like this hack...type-erasure forced this
      copied.asInstanceOf[E]
    }
  }

  private[services] def moveBatch[ID <: MusitId, E <: MoveEvent](
    mid: MuseumId,
    destination: StorageNodeDatabaseId,
    affectedIds: Seq[ID],
    eventuallyCurrentMap: Future[CurrLocType[ID]],
    moveEvents: Seq[E]
  )(
    mv: (GenericStorageNode, CurrLocType[ID], Seq[E]) => Future[MusitResult[Seq[ID]]]
  )(implicit ctId: ClassTag[ID], ctEvt: ClassTag[E]): Future[MusitResult[Seq[ID]]] = {
    val eventuallyEvents = for {
      maybeTo <- unitDao.getNodeById(mid, destination)
      // only continue if we have found the destination node
      if maybeTo.nonEmpty
      current <- eventuallyCurrentMap
    } yield {
      val ids = current.filterNot { t =>
        val isSame = t._2.contains(destination)
        if (isSame) logger.debug(s"${t._1} is already in node $destination")
        isSame
      }.keys.toVector

      val filtered = filterAndEnrich(current, ids, moveEvents)

      // Calling get on maybeTo should be safe, since we abort the for-comprehension
      // if it's nonEmpty
      (maybeTo.get, current, filtered) // scalastyle:ignore
    }

    eventuallyEvents.flatMap {
      case (to, current, events) if events.nonEmpty => mv(to, current, events)
      case _ => Future.successful(MusitValidationError("No move events to execute"))
    }
  }
}
