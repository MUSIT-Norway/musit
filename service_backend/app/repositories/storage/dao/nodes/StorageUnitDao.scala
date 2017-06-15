package repositories.storage.dao.nodes

import com.google.inject.{Inject, Singleton}
import models.storage.nodes.StorageType.RootLoanType
import models.storage.nodes._
import models.storage.nodes.dto.{StorageNodeDto, StorageUnitDto}
import no.uio.musit.MusitResults._
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models.ObjectTypes.CollectionObjectType
import no.uio.musit.models._
import no.uio.musit.time.Implicits._
import no.uio.musit.time.dateTimeNow
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.shared.dao.{SharedTables, StorageTables}
import slick.jdbc.GetResult

import scala.concurrent.Future

// scalastyle:off number.of.methods
/**
 * TODO: Document me!!!
 */
// TODO: Change public API methods to use MusitResult[A]
@Singleton
class StorageUnitDao @Inject()(val dbConfigProvider: DatabaseConfigProvider)
    extends StorageTables
    with SharedTables {

  import profile.api._

  val logger = Logger(classOf[StorageUnitDao])

  /**
   * Check to see if the node with the provided StorageNodeId exists or not.
   *
   * @param id
   * @return
   */
  def exists(mid: MuseumId, id: StorageNodeDatabaseId): Future[MusitResult[Boolean]] = {
    val query = storageNodeTable.filter { su =>
      su.museumId === mid && su.id === id && su.isDeleted === false
    }.exists.result

    db.run(query)
      .map(found => MusitSuccess(found))
      .recover(nonFatal("Checking if node exists caused an exception"))
  }

  def exists(mid: MuseumId, id: StorageNodeId): Future[MusitResult[Boolean]] = {
    val query = storageNodeTable.filter { su =>
      su.museumId === mid && su.uuid === id && su.isDeleted === false
    }.exists.result

    db.run(query)
      .map(found => MusitSuccess(found))
      .recover(nonFatal("Checking if node exists caused an exception"))
  }

  /**
   * Count of *all* children of this node, irrespective of access rights to
   * the children
   */
  def numChildren(id: StorageNodeDatabaseId): Future[MusitResult[Int]] = {
    db.run(countChildren(id))
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"An error occurred counting number node children under $id"))
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
    val q = localObjectsTable.filter(_.currentLocationId === nodeId).length

    db.run(q.result)
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"An error occurred counting number direct objects in $nodeId"))
  }

  /**
   * TODO: Document me!!!
   */
  def getByDatabaseId(
      mid: MuseumId,
      id: StorageNodeDatabaseId
  ): Future[MusitResult[Option[StorageUnit]]] = {
    val query = getNonRootByDatabaseIdAction(mid, id)
    db.run(query)
      .map(res => MusitSuccess(res.map(StorageNodeDto.toStorageUnit)))
      .recover(nonFatal(s"Unable to get storage unit with museimId $mid"))
  }

  def getById(
      mid: MuseumId,
      id: StorageNodeId
  ): Future[MusitResult[Option[StorageUnit]]] = {
    val query = getNonRootByIdAction(mid, id)
    db.run(query)
      .map(res => MusitSuccess(res.map(StorageNodeDto.toStorageUnit)))
      .recover(nonFatal(s"Unable to get storage unit with museimId $mid"))
  }

  /**
   * TODO: Document me!!!
   */
  def getNodeByDatabaseId(
      mid: MuseumId,
      id: StorageNodeDatabaseId
  ): Future[MusitResult[Option[GenericStorageNode]]] = {
    val query = getNodeByDatabaseIdAction(mid, id)
    db.run(query)
      .map(_.map(StorageNodeDto.toGenericStorageNode))
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"Unable to get nodes by id for museumId $mid"))
  }

  def getNodeById(
      mid: MuseumId,
      id: StorageNodeId
  ): Future[MusitResult[Option[GenericStorageNode]]] = {
    val query = getNodeByIdAction(mid, id)
    db.run(query)
      .map(_.map(StorageNodeDto.toGenericStorageNode))
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"Unable to get nodes by id for museumId $mid"))
  }

  def getParentsForNodes(
      mid: MuseumId,
      ids: Seq[StorageNodeId]
  ): Future[MusitResult[Map[StorageNodeId, Option[StorageNodeId]]]] = {
    val q1 = storageNodeTable.filter(n => n.museumId === mid && (n.uuid inSet ids))
    val query = for {
      (child, parent) <- q1 joinLeft storageNodeTable on (_.isPartOf === _.id)
    } yield {
      // It's safe to do a get on child.uuid here, because it wouldn't have been
      // found if it wasn't set. Besides it's _really_ a required column.
      child.uuid -> parent.map(_.uuid)
    }

    db.run(query.result)
      .map(res => MusitSuccess(res.map(r => r._1 -> r._2).toMap))
      .recover(nonFatal("Unexpected error occurred fetching parent nodes."))
  }

  /**
   * Fetches the node data for provided database ids
   *
   * @param mid
   * @param ids
   * @return
   */
  def getNodesByDatabaseIds(
      mid: MuseumId,
      ids: Seq[StorageNodeDatabaseId]
  ): Future[MusitResult[Seq[GenericStorageNode]]] = {
    val query = storageNodeTable.filter { sn =>
      sn.museumId === mid &&
      sn.isDeleted === false &&
      (sn.id inSet ids)
    }.result

    db.run(query)
      .map(_.map(StorageNodeDto.toGenericStorageNode))
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"Unable to get nodes by id for museumId $mid"))
  }

  def getNodesByIds(
      mid: MuseumId,
      ids: Seq[StorageNodeId]
  ): Future[MusitResult[Seq[GenericStorageNode]]] = {
    val query = storageNodeTable.filter { sn =>
      sn.museumId === mid &&
      sn.isDeleted === false &&
      (sn.uuid inSet ids)
    }.result

    db.run(query)
      .map(_.map(StorageNodeDto.toGenericStorageNode))
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"Unable to get nodes by id for museumId $mid"))
  }

  /**
   * TODO: Document me!!!
   */
  def getStorageTypesInPath(
      mid: MuseumId,
      path: NodePath,
      limit: Option[Int] = None
  ): Future[MusitResult[Seq[(StorageNodeDatabaseId, StorageType)]]] = {
    val ids = limit.map(l => path.asIdSeq.take(l)).getOrElse(path.asIdSeq)
    val query = storageNodeTable.filter { sn =>
      sn.museumId === mid &&
      sn.id.inSet(ids)
    }.map(res => (res.id, res.storageType, res.path)).sortBy(_._3.asc)

    db.run(query.result)
      .map(_.map(tuple => tuple._1 -> tuple._2))
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"Unable to get nodes by id for museumId $mid"))
  }

  /**
   * Find all nodes that are of type Root.
   *
   * @return a Future collection of Root nodes.
   */
  def findRootNodes(mid: MuseumId): Future[MusitResult[Seq[RootNode]]] = {
    val query = storageNodeTable.filter { root =>
      root.museumId === mid &&
      root.isDeleted === false &&
      root.isPartOf.isEmpty &&
      (root.storageType === rootNodeType || root.storageType === rootLoanType)
    }.result

    db.run(query)
      .map(_.map(StorageNodeDto.toRootNode))
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"Unable to find rood nodes for museumId $mid"))
  }

  /**
   * find the Root node with the given StorageNodeId.
   *
   * @param id StorageNodeId for the Root node.
   * @return An Option that contains the Root node if it was found.
   */
  def findRootNode(id: StorageNodeDatabaseId): Future[MusitResult[Option[RootNode]]] = {
    val query = storageNodeTable.filter { root =>
      root.id === id &&
      root.isDeleted === false &&
      (root.storageType === rootNodeType || root.storageType === rootLoanType)
    }.result.headOption

    db.run(query).map(mdto => MusitSuccess(mdto.map(StorageNodeDto.toRootNode)))
  }

  def findRootLoanNodes(
      museumId: MuseumId
  ): Future[MusitResult[Seq[StorageNodeDatabaseId]]] = {
    val tpe: StorageType = StorageType.RootLoanType
    val query = storageNodeTable.filter { n =>
      n.museumId === museumId && n.storageType === tpe
    }.map(_.id)

    db.run(query.result)
      .map(nodes => MusitSuccess(nodes))
      .recover(nonFatal(s"Error occurred getting RootLoan nodes for museum $museumId"))
  }

  private def sortedChildrenQuery(
      mid: MuseumId,
      id: StorageNodeId,
      page: Int,
      limit: Int
  )(implicit gr: GetResult[StorageUnitDto]) = {
    val offset = (page - 1) * limit

    sql"""
      SELECT sn.STORAGE_NODE_ID, sn.STORAGE_NODE_UUID, sn.STORAGE_NODE_NAME,
        sn.AREA, sn.AREA_TO, sn.IS_PART_OF, sn.HEIGHT, sn.HEIGHT_TO,
        sn.GROUP_READ, sn.GROUP_WRITE, sn.OLD_BARCODE, sn.NODE_PATH,
        sn.IS_DELETED, sn.STORAGE_TYPE, sn.MUSEUM_ID, sn.UPDATED_BY, sn.UPDATED_DATE
      FROM
        (
          SELECT n.STORAGE_NODE_ID FROM MUSARK_STORAGE.STORAGE_NODE n
          WHERE n.STORAGE_NODE_UUID=${id.asString}
        ) p,
        MUSARK_STORAGE.STORAGE_NODE sn
      WHERE sn.IS_PART_OF=p.STORAGE_NODE_ID
      AND sn.MUSEUM_ID=${mid.underlying}
      AND sn.IS_DELETED=0
      ORDER BY CASE
        WHEN sn.STORAGE_TYPE='Organisation' THEN '01'
        WHEN sn.STORAGE_TYPE='Building' THEN '02'
        WHEN sn.STORAGE_TYPE='Room' THEN '03'
        WHEN sn.STORAGE_TYPE='StorageUnit' THEN '04'
        ELSE sn.STORAGE_TYPE END ASC, lower(sn.STORAGE_NODE_NAME)
      OFFSET ${offset} ROWS FETCH NEXT ${limit} ROWS ONLY
    """.as[StorageUnitDto]
  }

  private def totalChildCountQuery(mid: MuseumId, id: StorageNodeId) = {
    sql"""
      SELECT COUNT(1) FROM
        (
          SELECT n.STORAGE_NODE_ID FROM MUSARK_STORAGE.STORAGE_NODE n
          WHERE n.STORAGE_NODE_UUID=${id.asString}
        ) p,
        MUSARK_STORAGE.STORAGE_NODE sn
      WHERE sn.MUSEUM_ID=${mid.underlying}
      AND sn.IS_PART_OF=p.STORAGE_NODE_ID
      AND sn.IS_DELETED=0
    """.as[Int].head
  }

  /**
   * Retrieve a page result of the children for the given {{{StorageNodeId}}}
   *
   * @param mid
   * @param id
   * @param page
   * @param limit
   * @return
   */
  def getChildren(
      mid: MuseumId,
      id: StorageNodeId,
      page: Int,
      limit: Int
  ): Future[MusitResult[PagedResult[GenericStorageNode]]] = {
    implicit val tupleToResult = StorageNodeDto.storageUnitTupleGetResult

    val totalChildrenQuery = totalChildCountQuery(mid, id)
    val sortedQuery        = sortedChildrenQuery(mid, id, page, limit)

    val matches: Future[MusitResult[Vector[GenericStorageNode]]] = db
      .run(sortedQuery)
      .map(_.map(StorageNodeDto.toGenericStorageNode))
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"Unexpected error when fetching children"))

    val total: Future[MusitResult[Int]] = db
      .run(totalChildrenQuery)
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"Unexpected error when fetching total children"))

    (for {
      mat <- MusitResultT(matches)
      tot <- MusitResultT(total)
    } yield PagedResult(tot, mat)).value
  }

  def listAllChildrenFor(
      museumId: MuseumId,
      ids: Seq[StorageNodeDatabaseId]
  ): Future[MusitResult[Seq[(StorageNodeDatabaseId, String)]]] = {
    val q1 = (likePath: String) =>
      storageNodeTable.filter { n =>
        n.museumId === museumId && (SimpleLiteral[String]("NODE_PATH") like likePath)
    }

    val query = ids
      .map(id => s",${id.underlying},%")
      .map(q1)
      .reduce((query, queryPart) => query union queryPart)
      .map(n => (n.id, n.name))
      .sortBy(_._2.asc)

    db.run(query.result)
      .map { res =>
        MusitSuccess(
          res.map(r => (r._1, r._2))
        )
      }
      .recover(
        nonFatal(
          s"Error occurred reading children for RootLoan nodes ${ids.mkString(", ")}"
        )
      )
  }

  /**
   * Get the StorageType for the given StorageNodeDatabaseId
   */
  def getStorageTypeFor(
      mid: MuseumId,
      id: StorageNodeDatabaseId
  ): Future[MusitResult[Option[StorageType]]] = {
    val query = storageNodeTable.filter { node =>
      node.id === id && node.isDeleted === false
    }.map(_.storageType).result.headOption

    db.run(query).map(MusitSuccess.apply)
  }

  /**
   * Get the StorageNodeDatabaseId and StorageType for the given StorageNodeId
   */
  def getStorageTypeFor(
      mid: MuseumId,
      uuid: StorageNodeId
  ): Future[MusitResult[Option[(StorageNodeDatabaseId, StorageType)]]] = {
    val query = storageNodeTable.filter { n =>
      n.museumId === mid && n.uuid === uuid && n.isDeleted === false
    }.map(n => n.id -> n.storageType).result.headOption

    db.run(query).map(MusitSuccess.apply)
  }

  /**
   * Get the StorageNodeDatabaseId and StorageType for the given old barcode.
   */
  def getStorageTypeFor(
      mid: MuseumId,
      oldBarcode: Long
  ): Future[MusitResult[Option[(StorageNodeDatabaseId, StorageType)]]] = {
    val query = storageNodeTable.filter { n =>
      n.museumId === mid && n.oldBarcode === oldBarcode && n.isDeleted === false
    }.map(n => n.id -> n.storageType).result.headOption

    db.run(query).map(MusitSuccess.apply)
  }

  /**
   * TODO: Document me!!!
   */
  def insert(
      mid: MuseumId,
      storageUnit: StorageUnit
  ): Future[MusitResult[StorageNodeDatabaseId]] = {
    val dto = StorageNodeDto.fromStorageUnit(mid, storageUnit)
    db.run(insertNodeAction(dto))
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"Unable to insert storage unit for museumId: $mid"))
  }

  /**
   * TODO: Document me!!!
   */
  def insertRoot(
      mid: MuseumId,
      root: RootNode
  ): Future[MusitResult[StorageNodeDatabaseId]] = {
    logger.debug("Inserting root node...")
    val dto = StorageNodeDto.fromRootNode(mid, root).asStorageUnitDto(mid)
    db.run(insertNodeAction(dto))
      .map(MusitSuccess.apply)
      .recover(nonFatal("Unable to insert root"))
  }

  /**
   * Set the path for the Root with the given StorageNodeId.
   *
   * @param id   StorageNodeId of the Root node.
   * @param path NodePath to set
   * @return An Option containing the updated Root node.
   */
  def setRootPath(
      id: StorageNodeDatabaseId,
      path: NodePath
  ): Future[MusitResult[Unit]] = {
    logger.debug(s"Updating path to $path for root node $id")
    db.run(updatePathAction(id, path)).map {
      case res: Int if res == 1 =>
        MusitSuccess(())

      case res: Int =>
        val msg = wrongNumUpdatedRows(id, res)
        logger.warn(msg)
        MusitDbError(msg)
    }
  }

  /**
   * Updates the path for all nodes that starts with the "oldPath".
   *
   * @param id   the StorageNodeId to update
   * @param path the NodePath to set
   * @return MusitResult[Unit]
   */
  def setPath(id: StorageNodeDatabaseId, path: NodePath): Future[MusitResult[Unit]] = {
    db.run(updatePathAction(id, path)).map {
      case res: Int if res == 1 =>
        MusitSuccess(())

      case res: Int =>
        val msg = wrongNumUpdatedRows(id, res)
        logger.warn(msg)
        MusitDbError(msg)
    }
  }

  /**
   * Updates all paths for the subtree of the given StorageNodeId
   *
   * @param id      StorageNodeId
   * @param oldPath NodePath representing the old path
   * @param newPath NodePath representing the new path
   * @return The number of paths updated.
   */
  def updateSubTreePath(
      id: StorageNodeDatabaseId,
      oldPath: NodePath,
      newPath: NodePath
  ): Future[MusitResult[Int]] = {
    db.run(updatePathsAction(oldPath, newPath).transactionally)
      .map {
        case res: Int if res != 0 =>
          logger.debug(s"Successfully updated path for $res nodes")
          MusitSuccess(res)

        case _ =>
          val msg = s"Did not update any paths starting with $oldPath"
          logger.error(msg)
          MusitInternalError(msg)

      }
      .recover(nonFatal(s"Unexpected error when updating paths for unit $id sub-tree"))
  }

  def batchUpdateLocation(
      nodes: Seq[StorageNode],
      newParent: StorageNode
  ): Future[MusitResult[Unit]] = {
    val a1 = DBIO.sequence(nodes.map(n => updatePartOfAction(n.id.get, newParent.id)))
    val a2 = DBIO.sequence {
      nodes.map(n => updatePathsAction(n.path, newParent.path.appendChild(n.id.get)))
    }

    db.run(a2.andThen(a1).transactionally)
      .map { _ =>
        logger.debug(s"Successfully updated node locations for ${nodes.size} nodes")
        MusitSuccess(())
      }
      .recover(
        nonFatal(s"Unexpected error when updating location for ${nodes.size} nodes.")
      )
  }

  /**
   * TODO: Document me!!!
   */
  def update(
      mid: MuseumId,
      id: StorageNodeId,
      storageUnit: StorageUnit
  ): Future[MusitResult[Option[Int]]] = {
    val dto = StorageNodeDto.fromStorageUnit(mid, storageUnit, uuid = Some(id))
    db.run(updateNodeAction(mid, id, dto)).map {
      case res: Int if res == 1 => MusitSuccess(Some(res))
      case res: Int if res == 0 => MusitSuccess(None)
      case res: Int =>
        val msg = wrongNumUpdatedRows(id, res)
        logger.warn(msg)
        MusitDbError(msg)
    }
  }

  /**
   * Find and return the NodePath for the given StorageNodeId.
   *
   * @param id StorageNodeId to get the NodePath for
   * @return NodePath
   */
  def getPathByDatabaseId(
      mid: MuseumId,
      id: StorageNodeDatabaseId
  ): Future[MusitResult[Option[NodePath]]] = {
    db.run(getPathByDatabaseIdAction(mid, id))
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"Unable to get path for museumId $mid and storage node $id"))
  }

  def getPathById(
      mid: MuseumId,
      id: StorageNodeId
  ): Future[MusitResult[Option[NodePath]]] = {
    db.run(getPathByIdAction(mid, id))
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"Unable to get path for museumId $mid and storage node $id"))
  }

  /**
   * TODO: Document me!!!
   */
  def markAsDeleted(
      doneBy: ActorId,
      mid: MuseumId,
      id: StorageNodeId
  ): Future[MusitResult[Int]] = {
    val query = storageNodeTable.filter { su =>
      su.uuid === id && su.isDeleted === false && su.museumId === mid
    }.map { del =>
      (del.isDeleted, del.updatedBy, del.updatedDate)
    }.update((true, Some(doneBy), Some(dateTimeNow)))

    db.run(query).map {
      case res: Int if res == 1 =>
        MusitSuccess(res)

      case res: Int =>
        val msg = wrongNumUpdatedRows(id, res)
        logger.warn(msg)
        MusitDbError(msg)
    }
  }

  /**
   * TODO: Document me!!!
   */
  def updatePartOf(
      id: StorageNodeDatabaseId,
      partOf: Option[StorageNodeDatabaseId]
  ): Future[MusitResult[Int]] = {
    val query = updatePartOfAction(id, partOf)

    db.run(query.transactionally).map {
      case res: Int if res == 1 =>
        MusitSuccess(res)

      case res: Int =>
        val msg = wrongNumUpdatedRows(id, res)
        logger.warn(msg)
        MusitDbError(msg)
    }
  }

  /**
   * Given the provided NodePath, fetch all the associated names for each of
   * the ID's in the path.
   *
   * @param nodePath NodePath to find names for
   * @return A Seq[NamedPathElement]
   */
  def namesForPath(nodePath: NodePath): Future[MusitResult[Seq[NamedPathElement]]] = {
    db.run(namesForPathAction(nodePath))
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"Unable to get node paths for $nodePath"))
  }

  /**
   *
   * @param mid
   * @param searchString
   * @param page
   * @param limit
   * @return
   */
  def getStorageNodeByName(
      mid: MuseumId,
      searchString: String,
      page: Int,
      limit: Int
  ): Future[MusitResult[Seq[GenericStorageNode]]] = {
    if (searchString.length > 2) {
      val query = getStorageNodeByNameAction(mid, searchString, page, limit)
      db.run(query)
        .map(_.map(StorageNodeDto.toGenericStorageNode))
        .map(MusitSuccess.apply)
        .recover(
          nonFatal(s"Unable to get node by name '$searchString' for museum: $mid")
        )

    } else {
      Future.successful(MusitSuccess(Seq.empty))
    }
  }

  def currentLocation(
      mid: MuseumId,
      objectId: ObjectUUID
  ): Future[MusitResult[Option[(StorageNodeId, NodePath)]]] = {
    val findLocalObjectAction = localObjectsTable.filter { lo =>
      lo.museumId === mid &&
      lo.objectUuid === objectId &&
      lo.objectType === CollectionObjectType.name
    }.map(_.currentLocationId).result.headOption

    val findPathAction = (maybeId: Option[StorageNodeId]) =>
      maybeId.map { nodeId =>
        storageNodeTable.filter(_.uuid === nodeId).map(_.path).result.headOption
      }.getOrElse(DBIO.successful(None))

    val query = for {
      maybeNodeId <- findLocalObjectAction
      maybePath   <- findPathAction(maybeNodeId)
    } yield maybeNodeId.flatMap(nid => maybePath.map(p => (nid, p)))

    db.run(query)
      .map(MusitSuccess.apply)
      .recover(
        nonFatal(s"Error occurred while getting current location for object $objectId")
      )
  }

}

// scalastyle:on number.of.methods
