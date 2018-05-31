package services.reporting

import com.google.inject.Inject
import models.reporting.NodeStats
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models.{MuseumId, StorageNodeId}
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import repositories.reporting.dao.StorageStatsDao
import repositories.storage.dao.nodes.StorageUnitDao

import scala.concurrent.{ExecutionContext, Future}

class StatsService @Inject()(
    implicit
    val nodeDao: StorageUnitDao,
    val statsDao: StorageStatsDao,
    val ec: ExecutionContext
) {

  val logger = Logger(classOf[StatsService])

  def nodeStats(
      mid: MuseumId,
      nodeId: StorageNodeId
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[NodeStats]]] = {
    MusitResultT(nodeDao.getPathById(mid, nodeId)).flatMap { maybeNodePath =>
      maybeNodePath.map { path =>
        val totalF                  = MusitResultT(statsDao.numObjectsInPath(path))
        val directF                 = MusitResultT(statsDao.numObjectsInNode(nodeId))
        val nodeCountF              = MusitResultT(statsDao.numChildren(nodeId))
        val samplesInCurrentNodeF   = MusitResultT(statsDao.numSamplesInNode(nodeId))
        val totalSamplesInNodePathF = MusitResultT(statsDao.numSamplesInPath(path))

        for {
          total                  <- totalF
          direct                 <- directF
          nodeCount              <- nodeCountF
          samplesInCurrentNode   <- samplesInCurrentNodeF
          totalSamplesInNodePath <- totalSamplesInNodePathF
        } yield {
          Option(
            NodeStats(
              nodeCount,
              direct,
              total,
              samplesInCurrentNode,
              totalSamplesInNodePath
            )
          )
        }
      }.getOrElse {
        MusitResultT[Future, Option[NodeStats]](Future.successful(MusitSuccess(None)))
      }
    }.value
  }

}
