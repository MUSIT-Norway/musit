package repositories.actor.dao

import com.google.inject.Inject
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import no.uio.musit.models.ObjectTypes.{CollectionObject, ObjectType}
import no.uio.musit.models._
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.control.NonFatal

class StorageNodeDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends Tables {

  private val logger = Logger(classOf[StorageNodeDao])

  import profile.api._

  def getPathById(
      mid: MuseumId,
      id: StorageNodeId
  ): Future[MusitResult[Option[(StorageNodeId, NodePath)]]] = {
    val q = nodeTable.filter { n =>
      n.museumId === mid && n.uuid === id
    }.result.headOption

    db.run(q).map(mres => MusitSuccess(mres.map(res => id -> res._14))).recover {
      case NonFatal(ex) =>
        val msg = s"Error occurred looking for nodeId $id"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

  def nodeExists(
      mid: MuseumId,
      nodeId: StorageNodeId
  ): Future[MusitResult[Boolean]] = {
    val query = nodeTable.filter { sn =>
      sn.museumId === mid &&
      sn.uuid === nodeId
    }.length.result
    db.run(query).map(res => MusitSuccess(res == 1)).recover {
      case NonFatal(ex) =>
        val msg = s"Error occurred while checking for node existence for nodeId $nodeId"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

  def currentLocation(
      mid: MuseumId,
      objectId: ObjectUUID
  ): Future[Option[(StorageNodeId, NodePath)]] = {
    val findLocalObjectAction = locObjTable.filter { lo =>
      lo.museumId === mid &&
      lo.objectUuid === objectId &&
      lo.objectType === CollectionObject.name
    }.map(_.currentLocationId).result.headOption

    val findPathAction = (maybeId: Option[StorageNodeId]) =>
      maybeId.map { nodeId =>
        nodeTable.filter(_.uuid === nodeId).map(_.path).result.headOption
      }.getOrElse(DBIO.successful(None))

    val query = for {
      maybeNodeId <- findLocalObjectAction
      maybePath   <- findPathAction(maybeNodeId)
    } yield maybeNodeId.flatMap(nid => maybePath.map(p => (nid, p)))

    db.run(query).recover {
      case NonFatal(ex) =>
        val msg = s"Error occurred while getting current location for object $objectId"
        logger.error(msg, ex)
        None
    }
  }

  def namesForPath(nodePath: NodePath): Future[Seq[NamedPathElement]] = {
    val query = nodeTable.filter { sn =>
      sn.id inSetBind nodePath.asIdSeq
    }.map(s => (s.id, s.uuid, s.name))
      .result
      .map(_.map(t => NamedPathElement(t._1, t._2, t._3)))
    db.run(query).recover {
      case NonFatal(ex) =>
        val msg = s"Error occurred while fetching named path for $nodePath"
        logger.error(msg, ex)
        Seq.empty
    }
  }

  def getRootLoanNodes(
      museumId: MuseumId
  ): Future[MusitResult[Seq[StorageNodeDatabaseId]]] = {
    val query = nodeTable.filter { n =>
      n.museumId === museumId && n.storageType === "RootLoan"
    }.map(_.id)

    db.run(query.result).map(nodes => MusitSuccess(nodes)).recover {
      case NonFatal(ex) =>
        val msg = s"Error occurred getting RootLoan nodes for museum $museumId"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

  def listAllChildrenFor(
      museumId: MuseumId,
      ids: Seq[StorageNodeDatabaseId]
  ): Future[MusitResult[Seq[(StorageNodeDatabaseId, String)]]] = {
    val q1 = (likePath: String) =>
      nodeTable.filter { n =>
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
      .recover {
        case NonFatal(ex) =>
          val msg = s"Error occurred reading children for RootLoan " +
            s"nodes ${ids.mkString(", ")}"
          logger.error(msg, ex)
          MusitDbError(msg, Option(ex))
      }
  }

}