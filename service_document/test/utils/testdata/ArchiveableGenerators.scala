package utils.testdata

import models.document.ArchiveTypes._
import net.scalytica.symbiotic.api.types.Path
import no.uio.musit.models.MuseumId

trait ArchiveableGenerators {

  // TODO: generators for Archive
  def generateArchive(
      mid: MuseumId,
      title: String,
      desc: Option[String] = None,
      path: Option[Path] = None
  ): Archive = {
    Archive(
      id = None,
      fid = None,
      title = title,
      description = desc,
      owner = None, // Will be set by DocumentArchiveService
      collection = None, // Will be set by DocumentArchiveService
      path = path.map(_.append(title)),
      lock = None,
      published = false,
      documentMedium = Some("digital"),
      closedStamp = None,
      createdStamp = None // Will be set by DocumentArchiveService
    )
  }

  // TODO: generators for ArchivePart
  // TODO: generators for ArchiveFolder
  // TODO: generators for ArchiveDocument
}
