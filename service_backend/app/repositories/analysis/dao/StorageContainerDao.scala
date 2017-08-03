package repositories.analysis.dao

import com.google.inject.{Inject, Singleton}
import models.analysis.StorageContainer
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.repositories.DbErrorHandlers
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

@Singleton
class StorageContainerDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends AnalysisTables
    with DbErrorHandlers {

  val logger = Logger(classOf[StorageContainerDao])

  import profile.api._

  def getStorageContainerList: Future[MusitResult[Seq[StorageContainer]]] = {
    db.run(storageContainerTable.result)
      .map(_.map(fromStorageContainerRow))
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"An unexpected error occurred fetching storage container list"))
  }
}
