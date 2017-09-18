package models.document

import net.scalytica.symbiotic.api.types.Path
import no.uio.musit.models.{MuseumId, Museums}

object BaseFolders {

  val MuseumArchivePaths: Map[MuseumId, Path] =
    Museums.museums.map(m => m.id -> Path.root.append(m.shortName)).toMap

  val ModulesFolderName = "Modules"
  val ModulesFolderPath = Path.root.append(ModulesFolderName)

  val ModuleFolders = Seq(
    AnalysisFolder,
    StorageFacilityFolder,
    LoanFolder,
    ConservationFolder
  )

  sealed trait ModuleFolder {
    val moduleName: String

    def path(museumId: MuseumId) = ModulesFolderPath.append(moduleName)
  }

  object AnalysisFolder extends ModuleFolder {
    override val moduleName = "Analysis"
  }

  object StorageFacilityFolder extends ModuleFolder {
    override val moduleName = "StorageFacility"
  }

  object LoanFolder extends ModuleFolder {
    override val moduleName = "Loan"
  }

  object ConservationFolder extends ModuleFolder {
    override val moduleName = "Conservation"
  }

}
