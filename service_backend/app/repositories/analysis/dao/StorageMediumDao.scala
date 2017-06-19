package repositories.analysis.dao

import com.google.inject.{Inject, Singleton}
import models.analysis.StorageMedium
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.shared.dao.DbErrorHandlers

import scala.concurrent.Future

@Singleton
class StorageMediumDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends AnalysisTables
    with DbErrorHandlers {

  val logger = Logger(classOf[StorageMediumDao])

  import profile.api._

  def getStorageMediumList: Future[MusitResult[Seq[StorageMedium]]] = {
    db.run(storageMediumTable.result)
      .map(_.map(fromStorageMediumRow))
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"An unexpected error occurred fetching storage medium list"))
  }

}
