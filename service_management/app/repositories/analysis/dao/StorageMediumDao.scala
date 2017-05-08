package repositories.analysis.dao

import com.google.inject.{Inject, Singleton}
import models.analysis.StorageMedium
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class StorageMediumDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends AnalysisTables {

  val logger = Logger(classOf[StorageMediumDao])

  import profile.api._

  def getStorageMediumList: Future[MusitResult[Seq[StorageMedium]]] = {
    db.run(storageMediumTable.result)
      .map(_.map(fromStorageMediumRow))
      .map(MusitSuccess.apply)
      .recover {
        case NonFatal(ex) =>
          val msg = s"An unexpected error occurred fetching storage medium list"
          logger.error(msg, ex)
          MusitDbError(msg, Option(ex))
      }
  }

}
