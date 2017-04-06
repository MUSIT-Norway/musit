package services

import models.event.envreq.EnvRequirement
import models.event.move._
import models.storage.StorageType._
import models.storage._
import no.uio.musit.MusitResults._
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models.NodePath.{empty => EmptyPath}
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

  // A couple of type aliases to reduce the length of some function args.
  // format: off
  // scalastyle:off line.size.limit
  type NodeInsertIO[A] = (MuseumId, A) => Future[MusitResult[StorageNodeDatabaseId]]
  type SetEnvReq[A] = (A, Option[EnvironmentRequirement]) => A
  type NodeUpdateIO[A] = (StorageNodeDatabaseId, NodePath) => Future[MusitResult[Unit]]
  type GetNodeIO[A] = (MuseumId, StorageNodeDatabaseId) => Future[MusitResult[Option[A]]]
  type CopyNode[A <: StorageNode] = (A, Option[EnvironmentRequirement], Option[Seq[NamedPathElement]]) => A
  type CurrLocType[ID] = Map[ID, Option[StorageNodeDatabaseId]]
  // format: on
  // scalastyle:on line.size.limit

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
        val er  = EnvRequirement.toEnvRequirementEvent(currUsr.id, nodeId, now, envReq)

        envReqService.add(mid, er).map {
          case MusitSuccess(success) =>
            logger.debug(
              "Successfully wrote environment requirement data " +
                s"for node $nodeId"
            )
            Some(EnvRequirement.fromEnvRequirementEvent(er))

          case err: MusitError =>
            logger.error(
              "Something went wrong while storing the environment " +
                s"requirements for node $nodeId"
            )
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
  ): Future[MusitResult[Option[NodePath]]] = {
    maybeId
      .map(id => unitDao.getPathById(mid, id))
      .getOrElse(Future.successful(MusitSuccess(None)))
  }

  /**
   * Helper method to find PathNames for a potentially present StorageNodeId.
   *
   * @param mid     MuseumId
   * @param maybeId Option[StorageNodeId]
   * @return {{{Future[MusitResult[(NodePath, Seq[NamedPathElement])]]}}}
   */
  private[services] def findPathAndNames(
      mid: MuseumId,
      maybeId: Option[StorageNodeDatabaseId]
  ): Future[MusitResult[(NodePath, Seq[NamedPathElement])]] = {

    def findNodes(
        maybePath: Option[NodePath]
    ): Future[MusitResult[(NodePath, Seq[NamedPathElement])]] = {
      maybePath.map { p =>
        unitDao.namesForPath(p).map {
          case MusitSuccess(names) => MusitSuccess((p, names))
          case err: MusitError     => err
        }
      }.getOrElse(Future.successful(MusitSuccess((EmptyPath, Seq()))))
    }

    val res = for {
      maybePath <- MusitResultT(findPath(mid, maybeId))
      nodes     <- MusitResultT(findNodes(maybePath))
    } yield nodes

    res.value
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
      val eventuallyNode  = MusitResultT(unitDao.numChildren(nodeId))

      val emptyNode = for {
        total     <- eventuallyTotal
        nodeCount <- eventuallyNode
      } yield (total + nodeCount) == 0

      emptyNode.value.map(_.toOption.getOrElse(false))

    }.getOrElse(Future.successful(false))
  }

  private[this] def validateMoveLocation[T <: StorageNode](
      idTypeTuples: Seq[(StorageNodeDatabaseId, StorageType)],
      maybeDestId: Option[StorageNodeDatabaseId],
      node: T,
      dest: NodePath
  ): Future[MusitResult[Unit]] = Future.successful {
    logger.debug(
      s"Found types for node IDs in destination path" +
        s": ${idTypeTuples.mkString(", ")}"
    )
    logger.trace(s"Validating destination for ${node.storageType.entryName}...")
    // Identify the type of node we want to place in the hierarchy, and
    // validate if the destination location is valid for the given type.
    val res = node.storageType match {
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

    if (!res) {
      logger.warn(
        s"Cannot move node ${node.id} to ${dest.path} because " +
          "it is not allowed"
      )
      MusitValidationError("Illegal move")
    } else {
      MusitSuccess(())
    }
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
  private[services] def validatePosition[T <: StorageNode](
      mid: MuseumId,
      node: T,
      dest: NodePath
  ): Future[MusitResult[Unit]] = {
    if (!dest.childOf(node.path)) {
      val maybeDestId = dest.asIdSeq.lastOption
      // Get the StorageType for the elements in the destination path so we can
      // use it to verify that nodes are placed on a valid location
      (for {
        tuples <- MusitResultT(unitDao.getStorageTypesInPath(mid, dest))
        res    <- MusitResultT(validateMoveLocation(tuples, maybeDestId, node, dest))
      } yield res).value
    } else {
      logger.warn(
        s"destination ($dest) is not allowed for ${node.id} because " +
          s"it is a child of ${node.path}."
      )
      Future.successful(MusitValidationError("Illegal destination"))
    }
  }

  /**
   * Validates nodes to be moved to see if the destination is valid considering
   * the type of each node. Any invalid moves will be filtered away, and the
   * valid ones are returned.
   *
   * @param mid   MuseumId
   * @param nodes the nodes to validate
   * @param dest  the destination node path
   * @return
   */
  private[services] def filterInvalidPosition(
      mid: MuseumId,
      dest: NodePath,
      nodes: Seq[GenericStorageNode]
  ): Future[Seq[GenericStorageNode]] = {
    Future.sequence {
      nodes.map { node =>
        validatePosition(mid, node, dest).map {
          case MusitSuccess(_) => Some(node)
          case _               => None
        }
      }
    }.map(_.filter(_.isDefined).map(_.get))
  }

  private val futureFilterErr = "Future.filter predicate is not satisfied"

  // scalastyle:off method.length
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

    def saveEnvReqForNode(
        node: T,
        nodeId: StorageNodeDatabaseId
    ): Future[MusitResult[Option[EnvironmentRequirement]]] =
      node.environmentRequirement
        .map(er => saveEnvReq(mid, nodeId, er))
        .getOrElse(Future.successful(None))
        .map(MusitSuccess.apply)

    val res = for {
      mPath <- MusitResultT(findPath(mid, node.isPartOf))
      _     <- MusitResultT(validatePosition(mid, node, mPath.getOrElse(EmptyPath)))
      // Call the insert function to persist the node if the path is valid.
      nodeId <- MusitResultT(insert(mid, node))
      _ <- MusitResultT(
            updateWithPath(nodeId, mPath.getOrElse(EmptyPath).appendChild(nodeId))
          )
      _       <- MusitResultT(saveEnvReqForNode(node, nodeId))
      theNode <- MusitResultT(getNode(mid, nodeId))
    } yield {
      logger.debug(
        s"Successfully added node ${node.name} of type" +
          s" ${node.storageType} to ${mPath.getOrElse(EmptyPath)}"
      )
      theNode
    }

    res.value.recover {
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

  // scalastyle:on method.length

  private[services] def getEnvReq(
      mid: MuseumId,
      id: StorageNodeDatabaseId
  ): Future[MusitResult[Option[EnvironmentRequirement]]] = {
    envReqService.findLatestForNodeId(mid, id).recover {
      case NonFatal(ex) =>
        // If we fail fetching the envreq event, we'll return None.
        logger.warn(
          "Something went wrong trying to locate latest " +
            s"environment requirement data for unit $id",
          ex
        )
        MusitSuccess(None)
    }
  }

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
      eventuallyMaybeNode: Future[MusitResult[Option[A]]]
  )(cp: CopyNode[A]): Future[MusitResult[Option[A]]] = {
    val eventuallyMaybeEnvReq = getEnvReq(mid, id)
    (for {
      maybeNode   <- MusitResultT(eventuallyMaybeNode)
      maybeEnvReq <- MusitResultT(eventuallyMaybeEnvReq)
      namedPathElems <- MusitResultT(maybeNode.map { node =>
                         unitDao.namesForPath(node.path)
                       }.getOrElse(Future.successful(MusitSuccess(Seq.empty))))
    } yield {
      val maybePathElems = {
        if (namedPathElems.nonEmpty) Some(namedPathElems)
        else None
      }
      maybeNode.map(n => cp(n, maybeEnvReq, maybePathElems))
    }).value
  }

  private def filterAndEnrich[ID <: MusitId, E <: MoveEvent](
      current: CurrLocType[ID],
      ids: Vector[ID],
      moveEvents: Seq[E]
  )(implicit ctId: ClassTag[ID], ctEvt: ClassTag[E]): Seq[E] = {
    moveEvents.filter(_.affectedThing.exists(ids.contains)).map { e =>
      val currId = e.affectedThing.get.asInstanceOf[ID]
      // scalastyle:ignore
      val id = current.get(currId).flatten
      // need to match on type to be able to access the copy function.
      val copied = e match {
        case obj: MoveObject => obj.copy(from = id)
        case nde: MoveNode   => nde.copy(from = id)
      }

      logger.debug(s"Copied from: ${copied.from} to: ${copied.to}")

      // Don't like this explicit type casting. Type-erasure forced this, but
      // it would be nice to find away around it.
      copied.asInstanceOf[E]
    }
  }

  private[services] def moveBatch[ID <: MusitId, E <: MoveEvent](
      mid: MuseumId,
      destination: StorageNodeDatabaseId,
      current: CurrLocType[ID],
      moveEvents: Seq[E]
  )(
      mv: (GenericStorageNode, CurrLocType[ID], Seq[E]) => Future[MusitResult[Seq[ID]]]
  )(implicit ctId: ClassTag[ID], ctEvt: ClassTag[E]): Future[MusitResult[Seq[ID]]] = {

    def filteredEvents(): Future[MusitResult[Seq[E]]] = {
      val ids = current.filterNot { t =>
        val isSame = t._2.contains(destination)
        if (isSame) logger.debug(s"${t._1} is already in node $destination")
        isSame
      }.keys.toVector

      val filtered = filterAndEnrich(current, ids, moveEvents)

      if (filtered.nonEmpty) Future.successful(MusitSuccess(filtered))
      else Future.successful(MusitValidationError("No move events to execute"))
    }

    val eventuallyEvents = for {
      maybeTo <- MusitResultT(unitDao.getNodeById(mid, destination))
      to <- MusitResultT(
             Future.successful(
               MusitResult.getOrError(
                 opt = maybeTo,
                 err = MusitValidationError("Didn't find the node")
               )
             )
           )
      events <- MusitResultT(filteredEvents())
      moved  <- MusitResultT(mv(to, current, events))
    } yield moved

    eventuallyEvents.value
  }
}
