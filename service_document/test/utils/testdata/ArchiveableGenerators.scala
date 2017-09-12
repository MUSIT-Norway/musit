package utils.testdata

import models.document.ArchiveTypes._
import no.uio.musit.models.MuseumId

trait ArchiveableGenerators {

  // TODO: generators for Archive
  def generateArchive(
      mid: MuseumId,
      title: String,
      desc: Option[String] = None
  ): Archive = {
    Archive(
      id = None,
      fid = None,
      title = title,
      description = desc,
      owner = None, // Will be set by DocumentArchiveService
      collection = None, // Will be set by DocumentArchiveService
      path = None, // Will be set by DocumentArchiveService
      lock = None,
      published = false,
      documentMedium = Some("digital"),
      closedStamp = None,
      createdStamp = None // Will be set by DocumentArchiveService
    )
  }

  def generateArchivePart(
      mid: MuseumId,
      title: String,
      desc: Option[String] = None
  ): ArchivePart = {
    ArchivePart(
      id = None,
      fid = None,
      title = title,
      description = desc,
      owner = None, // Will be set by DocumentArchiveService
      collection = None, // Will be set by DocumentArchiveService
      path = None, // Will be set by DocumentArchiveService
      lock = None,
      published = false,
      documentMedium = Some("digital"),
      closedStamp = None,
      createdStamp = None // Will be set by DocumentArchiveService
    )
  }

  def generateArchiveFolder(
      mid: MuseumId,
      title: String,
      desc: Option[String] = None
  ): ArchiveFolder = {
    ArchiveFolder(
      id = None,
      fid = None,
      title = title,
      description = desc,
      owner = None, // Will be set by DocumentArchiveService
      collection = None, // Will be set by DocumentArchiveService
      path = None, // Will be set by DocumentArchiveService
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
