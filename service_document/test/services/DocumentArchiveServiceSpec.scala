package services

import models.document.{ArchiveAddContext, ArchiveContext}
import models.document.ArchiveTypes.{Archive, ArchivePart, ArchiveRoot}
import net.scalytica.symbiotic.api.types.{FolderId, Path}
import net.scalytica.symbiotic.test.specs.PostgresSpec
import no.uio.musit.models.{MuseumCollections, MuseumId, Museums}
import no.uio.musit.test.matchers.MusitResultValues
import no.uio.musit.test.{
  MusitSpecWithAppPerSuite,
  ElasticsearchContainer => ES,
  PostgresContainer => PG
}
import org.scalatest.time.{Millis, Span}
import utils.testdata.{ArchiveableGenerators, BaseDummyData}

import scala.util.Properties.envOrNone

class DocumentArchiveServiceSpec
    extends MusitSpecWithAppPerSuite
    with PostgresSpec
    with BaseDummyData
    with ArchiveableGenerators
    with MusitResultValues {

  override val timeout =
    envOrNone("MUSIT_FUTURE_TIMEOUT").map(_.toDouble).getOrElse(5 * 1000d)
  override val interval =
    envOrNone("MUSIT_FUTURE_INTERVAL").map(_.toDouble).getOrElse(15d)

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(timeout, Millis),
    interval = Span(interval, Millis)
  )

  private[this] val service = fromInstanceCache[DocumentArchiveService]

  // default contexts to use for tests
  implicit val ctx: ArchiveContext       = dummyContext
  implicit val addCtx: ArchiveAddContext = dummyAddContext

  // Contexts for KHM. Must be explicitly passed to be picked up.
  val khmCtx = ArchiveContext(dummyUser, Museums.Khm.id)
  val khmAddCtx =
    ArchiveAddContext(dummyUser, Museums.Khm.id, MuseumCollections.Archeology.uuid)

  private[this] val folders = List.newBuilder[(MuseumId, FolderId, String)]

  def folderAdded(mid: MuseumId, fid: FolderId, tpe: String): Unit = {
    folders += ((mid, fid, tpe))
  }

  private[this] def getRootId(mid: MuseumId): FolderId = {
    folders.result().filter(t => t._1 == mid && t._3 == "root").head._2
  }

  private[this] def getArchiveId(mid: MuseumId, idx: Int): FolderId = {
    folders.result().filter(t => t._1 == mid && t._3 == Archive.FolderType)(idx)._2
  }

  private[this] def getArchivePartId(mid: MuseumId, idx: Int): FolderId = {
    folders.result().filter(t => t._1 == mid && t._3 == ArchivePart.FolderType)(idx)._2
  }

  "The DocumentArchiveService" should {

    "initialize the root directory for the test museum" taggedAs (PG, ES) in {
      val res = service.initRootFor(defaultMuseumId).futureValue.successValue
      res must not be empty
      println(s"TEST root ${res.get}")
      folderAdded(defaultMuseumId, res.value, "root")
    }

    "initialize the root directory for KHM" taggedAs (PG, ES) in {
      val res = service.initRootFor(Museums.Khm.id)(khmAddCtx).futureValue.successValue
      res must not be empty
      println(s"KHM root ${res.get}")
      folderAdded(defaultMuseumId, res.value, "root")
    }

    "get the root folder for the Test museum" in {
      val res = service.rootFolder(defaultMuseumId).futureValue.successValue
      res must not be empty
      res.value.title mustBe "root"
      res.value.metadata.fid.value mustBe getRootId(defaultMuseumId)
    }

    "add a few Archive folders to the root" taggedAs (PG, ES) in {
      val root = getRootId(defaultMuseumId)
      val archive = generateArchive(
        defaultMuseumId,
        "archive 1",
        Some("test archive 1"),
        Some(Path.root)
      )

      val res = service.addFolder(defaultMuseumId, root, archive).futureValue.successValue
      res must not be empty

      folderAdded(defaultMuseumId, res.value, Archive.FolderType)
    }

  }

}
