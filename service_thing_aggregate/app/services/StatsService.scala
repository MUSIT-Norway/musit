package services

import com.google.inject.Inject
import models.NodeStats
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models.{MuseumId, StorageNodeId}
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.dao.{StorageNodeDao, StorageStatsDao}

import scala.concurrent.Future

class StatsService @Inject()(
    val nodeDao: StorageNodeDao,
    val statsDao: StorageStatsDao
) {

  val logger = Logger(classOf[StatsService])

  def nodeStats(
      mid: MuseumId,
      nodeId: StorageNodeId
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[NodeStats]]] = {
    MusitResultT(nodeDao.getPathById(mid, nodeId)).flatMap { maybeNode =>
      maybeNode.map { node =>
        val totalF     = MusitResultT(statsDao.numObjectsInPath(node._2))
        val directF    = MusitResultT(statsDao.numObjectsInNode(node._1))
        val nodeCountF = MusitResultT(statsDao.numChildren(node._1))

        for {
          total     <- totalF
          direct    <- directF
          nodeCount <- nodeCountF
        } yield {
          Option(NodeStats(nodeCount, direct, total))
        }
      }.getOrElse {
        MusitResultT[Future, Option[NodeStats]](Future.successful(MusitSuccess(None)))
      }
    }.value
  }

}
