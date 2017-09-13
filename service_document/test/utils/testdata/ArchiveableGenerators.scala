package utils.testdata

import java.io.{File => JFile}

import akka.stream.scaladsl.FileIO
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

  def generateArchiveDocument(
      mid: MuseumId,
      author: String,
      title: String,
      desc: Option[String] = None
  ): ArchiveDocument = {
    val fileUri    = getClass.getClassLoader.getResource("test_files/clean.pdf").toURI
    val filePath   = new JFile(fileUri).toPath
    val fileSource = FileIO.fromPath(filePath)

    ArchiveDocument(
      id = None,
      fid = None,
      title = title,
      size = None,
      fileType = Some("application/pdf"),
      description = desc,
      owner = None, // Will be set by DocumentArchiveService
      collection = None, // Will be set by DocumentArchiveService
      path = None, // Will be set by DocumentArchiveService
      lock = None,
      version = 1,
      published = false,
      documentMedium = Some("digital"),
      createdStamp = None, // Will be set by DocumentArchiveService
      author = Some(author),
      documentDetails = DocumentDetails(
        docType = Some("foo"),
        docSubType = Some("bar")
      ),
      stream = Some(fileSource)
    )
  }
}
