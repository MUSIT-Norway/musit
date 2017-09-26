package modules

import com.google.inject.{Inject, Singleton}
import models.document.ArchiveIdentifiers.{ArchiveOwnerId, ArchiveUserId}
import models.document.{Archive, ArchiveAddContext, BaseFolders, GenericFolder}
import net.codingwell.scalaguice.ScalaModule
import net.scalytica.symbiotic.api.types.{FolderId, Path}
import net.scalytica.symbiotic.api.types.ResourceParties.Owner
import net.scalytica.symbiotic.core.DocManagementService
import no.uio.musit.MusitResults.{MusitGeneralError, MusitResult, MusitSuccess}
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.OptionT
import no.uio.musit.models.Museums.Museum
import no.uio.musit.models.{MuseumId, Museums}
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ArchiveBootstrapping extends ScalaModule {
  override def configure(): Unit = {
    bind[ArchiveBootstrapper].to[Bootstrapper].asEagerSingleton()
  }

}

trait ArchiveBootstrapper {
  protected val log = Logger("module.ArchiveBootstrapper")

  val dmService: DocManagementService

  def init(
      mid: MuseumId
  ): Future[MusitResult[FolderId]] = {
    val currUsr = ArchiveUserId.empty
    val owner   = Owner(ArchiveOwnerId(mid))

    implicit val ctx = ArchiveAddContext(currUsr, owner, Seq.empty)

    val res = for {
      root  <- OptionT(dmService.createRootFolder)
      arch  <- OptionT(initArchive(mid))
      mbase <- OptionT(initModuleFolder(mid))
      mods  <- OptionT(initModules(mid))
    } yield root

    res.value.map {
      case Some(root) =>
        MusitSuccess(root)

      case None =>
        MusitGeneralError(
          s"There was a problem initialising the base folder structure for $mid"
        )
    }
  }

  private def initModuleFolder(mid: MuseumId)(implicit ad: ArchiveAddContext) =
    dmService.createFolder(GenericFolder(BaseFolders.ModulesFolderPath).enrich()).map {
      case Some(id) =>
        log.info(s"Folder ${BaseFolders.ModulesFolderPath} was created for $mid")
        Some(id)

      case None =>
        log.error(s"Folder ${BaseFolders.ModulesFolderPath} was not created for $mid")
        None
    }

  private def initModules(mid: MuseumId)(implicit ad: ArchiveAddContext) =
    Future.sequence {
      BaseFolders.ModuleFolders.map { mf =>
        dmService.createFolder(GenericFolder(mf.path(mid)).enrich()).map { mfid =>
          if (mfid.isEmpty) log.error(s"Folder ${mf.path(mid)} was not created for $mid")
          else log.info(s"Folder ${mf.path(mid)} was created for $mid")
          mfid
        }
      }
    }.map { mfids =>
      if (mfids.exists(_.isEmpty)) {
        log.error(s"Not all base folders were created for $mid")
        None
      } else {
        Some(mfids.size)
      }
    }

  private def initArchive(mid: MuseumId)(implicit ad: ArchiveAddContext) = {
    Museum
      .fromMuseumId(mid)
      .map { museum =>
        val p       = Path.root.append(museum.shortName)
        val archive = Archive(museum.shortName).copy(path = Some(p)).enrich()

        dmService.createFolder(archive).map {
          case Some(id) =>
            log.info(s"Created Archive $id for $mid")
            Some(id)

          case None =>
            log.error(s"Archive was not created for $mid")
            None
        }
      }
      .getOrElse(Future.successful(None))
  }
}

@Singleton
class Bootstrapper @Inject()(
    val dmService: DocManagementService
) extends ArchiveBootstrapper {

  log.info(s"Initialising base folder structure for Museums")
  Museums.museums.filterNot(_ == Museums.All).map(m => init(m.id))
}

object Bootstrapper {

  def init(docManagementService: DocManagementService) = {
    val b = new ArchiveBootstrapper {
      override val dmService = docManagementService
    }

    val res = Museums.museums.filterNot(_ == Museums.All).map(m => b.init(m.id))

    Future.sequence(res)
  }

}
