package services

import models.document.ArchiveIdentifiers.ArchiveOwnerId
import models.document.ArchiveTypes.{Archive, ArchiveFolder, ArchivePart}
import models.document.Archiveables.ArchiveFolderItem
import models.document.{ArchiveAddContext, ArchiveContext, ArchiveTypes}
import net.scalytica.symbiotic.api.types.{FolderId, Path}
import net.scalytica.symbiotic.test.specs.PostgresSpec
import no.uio.musit.MusitResults.MusitGeneralError
import no.uio.musit.models.{MuseumCollections, MuseumId, Museums}
import no.uio.musit.test.matchers.MusitResultValues
import no.uio.musit.test.{MusitSpecWithAppPerSuite, PostgresContainer => PG}
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

  private[this] def getArchiveFolderId(mid: MuseumId, idx: Int): FolderId = {
    folders.result().filter(t => t._1 == mid && t._3 == ArchiveFolder.FolderType)(idx)._2
  }

  def failRenameFolderTest[A <: ArchiveFolderItem](
      parentId: FolderId,
      afi: A,
      existingFolderName: String
  ) = {
    val aid =
      service.addFolder(defaultMuseumId, parentId, afi).futureValue.successValue.value
    folderAdded(defaultMuseumId, aid, ArchiveTypes.asTypeString(afi))

    service.renameFolder(defaultMuseumId, aid, existingFolderName).futureValue match {
      case MusitGeneralError(err) =>
        err must include("There is already a folder")

      case bad =>
        fail(s"Expected MusitGeneralError, got ${bad.getClass}")
    }
  }

  def successRenameFolderTest[A <: ArchiveFolderItem](
      parentId: FolderId,
      afi: A,
      name: String
  ) = {
    val aid =
      service.addFolder(defaultMuseumId, parentId, afi).futureValue.successValue.value
    folderAdded(defaultMuseumId, aid, ArchiveTypes.asTypeString(afi))

    val ap = service.getFolder(defaultMuseumId, aid).futureValue.successValue.value

    val res = service.renameFolder(defaultMuseumId, aid, name).futureValue.successValue

    res must contain(ap.path.value.parent.append(name))
  }

  "The DocumentArchiveService" should {

    "initialize the root directory for the test museum" taggedAs PG in {
      val res = service.initRootFor(defaultMuseumId).futureValue.successValue
      res must not be empty
      folderAdded(defaultMuseumId, res.value, "root")
    }

    "initialize the root directory for KHM" taggedAs PG in {
      val res = service.initRootFor(Museums.Khm.id)(khmAddCtx).futureValue.successValue
      res must not be empty
      folderAdded(defaultMuseumId, res.value, "root")
    }

    "get the root folder for the Test museum" taggedAs PG in {
      val res = service.rootFolder(defaultMuseumId).futureValue.successValue
      res must not be empty
      res.value.title mustBe "root"
      res.value.metadata.fid.value mustBe getRootId(defaultMuseumId)
    }

    "add an Archive folder to the root" taggedAs PG in {
      val root = getRootId(defaultMuseumId)
      val archive = generateArchive(
        mid = defaultMuseumId,
        title = "archive 1",
        desc = Some("test archive 1")
      )

      val res = service.addFolder(defaultMuseumId, root, archive).futureValue.successValue
      res must not be empty

      folderAdded(defaultMuseumId, res.value, Archive.FolderType)
    }

    "not allow adding an Archive to an Archive" taggedAs PG in {
      val archiveId = getArchiveId(defaultMuseumId, 0)
      val archive = generateArchive(
        mid = defaultMuseumId,
        title = "archive_fail"
      )

      service.addFolder(defaultMuseumId, archiveId, archive).futureValue match {
        case MusitGeneralError(err) =>
          err must include("invalid location")

        case bad => fail(s"Expected MusitGeneralError but got ${bad.getClass}")
      }
    }

    "not allow adding an ArchivePart folder to the root" taggedAs PG in {
      val root = getRootId(defaultMuseumId)
      val archivePart = generateArchivePart(
        mid = defaultMuseumId,
        title = "archive part",
        desc = Some("test archive part")
      )

      service.addFolder(defaultMuseumId, root, archivePart).futureValue match {
        case MusitGeneralError(err) =>
          err must include("invalid location")

        case bad => fail(s"Expected MusitGeneralError but got ${bad.getClass}")
      }
    }

    "not allow adding an ArchiveFolder to the root" taggedAs PG in {
      val root = getRootId(defaultMuseumId)
      val archiveFolder = generateArchiveFolder(
        mid = defaultMuseumId,
        title = "archive folder",
        desc = Some("test archive folder")
      )

      service.addFolder(defaultMuseumId, root, archiveFolder).futureValue match {
        case MusitGeneralError(err) =>
          err must include("invalid location")

        case bad => fail(s"Expected MusitGeneralError but got ${bad.getClass}")
      }
    }

    "add an ArchivePart to an Archive" taggedAs PG in {
      val archiveId = getArchiveId(defaultMuseumId, 0)
      val archivePart = generateArchivePart(
        mid = defaultMuseumId,
        title = "archive part 1",
        desc = Some("test archive part 1")
      )

      val res = service
        .addFolder(defaultMuseumId, archiveId, archivePart)
        .futureValue
        .successValue
      res must not be empty

      folderAdded(defaultMuseumId, res.value, ArchivePart.FolderType)
    }

    "not allow adding an Archive to an ArchivePart" taggedAs PG in {
      val partId = getArchivePartId(defaultMuseumId, 0)
      val archive = generateArchive(
        mid = defaultMuseumId,
        title = "archive_fail"
      )

      service.addFolder(defaultMuseumId, partId, archive).futureValue match {
        case MusitGeneralError(err) =>
          err must include("invalid location")

        case bad => fail(s"Expected MusitGeneralError but got ${bad.getClass}")
      }
    }

    "not allow adding an ArchivePart to an ArchivePart" taggedAs PG in {
      pending // TODO: Not sure about this...need to read the specs again
    }

    "add an ArchiveFolder to an ArchivePart" taggedAs PG in {
      val partId = getArchivePartId(defaultMuseumId, 0)
      val folder = generateArchiveFolder(
        mid = defaultMuseumId,
        title = "archive folder 1"
      )

      val res =
        service.addFolder(defaultMuseumId, partId, folder).futureValue.successValue
      res must not be empty

      folderAdded(defaultMuseumId, res.value, ArchiveFolder.FolderType)
    }

    "not allow adding an Archive to an ArchiveFolder" taggedAs PG in {
      val folderId = getArchiveFolderId(defaultMuseumId, 0)
      val archive = generateArchive(
        mid = defaultMuseumId,
        title = "archive_fail"
      )

      service.addFolder(defaultMuseumId, folderId, archive).futureValue match {
        case MusitGeneralError(err) =>
          err must include("invalid location")

        case bad => fail(s"Expected MusitGeneralError but got ${bad.getClass}")
      }
    }

    "not allow adding an ArchivePart to an ArchiveFolder" taggedAs PG in {
      val folderId = getArchiveFolderId(defaultMuseumId, 0)
      val part = generateArchivePart(
        mid = defaultMuseumId,
        title = "archive_part_fail"
      )

      service.addFolder(defaultMuseumId, folderId, part).futureValue match {
        case MusitGeneralError(err) =>
          err must include("invalid location")

        case bad => fail(s"Expected MusitGeneralError but got ${bad.getClass}")
      }
    }

    "add an ArchiveFolder to another ArchiveFolder" taggedAs PG in {
      val folderId = getArchiveFolderId(defaultMuseumId, 0)
      val folder = generateArchiveFolder(
        mid = defaultMuseumId,
        title = "archive folder 1"
      )

      val res =
        service.addFolder(defaultMuseumId, folderId, folder).futureValue.successValue
      res must not be empty

      folderAdded(defaultMuseumId, res.value, ArchiveFolder.FolderType)
    }

    "not add an ArchiveFolder with name of existing ArchiveFolder" taggedAs PG in {
      val folderId = getArchiveFolderId(defaultMuseumId, 0)
      val folder = generateArchiveFolder(
        mid = defaultMuseumId,
        title = "archive folder 1"
      )

      service.addFolder(defaultMuseumId, folderId, folder).futureValue match {
        case MusitGeneralError(err) =>
          err must include("already exists")

        case bad =>
          fail(s"Expected MusitGeneralError but got ${bad.getClass}")
      }
    }

    "get an Archive by id" taggedAs PG in {
      val archiveId = getArchiveId(defaultMuseumId, 0)

      val res = service.getFolder(defaultMuseumId, archiveId).futureValue.successValue
      res must not be empty

      val a = res.get
      a mustBe an[Archive]
      a.title mustBe "archive 1"
      a.fid mustBe Some(archiveId)
      a.owner.value.id mustBe ArchiveOwnerId(defaultMuseumId)
    }

    "get an ArchivePart by id" taggedAs PG in {
      val partId = getArchivePartId(defaultMuseumId, 0)

      val res = service.getFolder(defaultMuseumId, partId).futureValue.successValue
      res must not be empty

      val ap = res.get
      ap mustBe an[ArchivePart]
      ap.title mustBe "archive part 1"
      ap.fid mustBe Some(partId)
      ap.owner.value.id mustBe ArchiveOwnerId(defaultMuseumId)
    }

    "get an ArchiveFolder by id" taggedAs PG in {
      val folderId = getArchiveFolderId(defaultMuseumId, 0)

      val res = service.getFolder(defaultMuseumId, folderId).futureValue.successValue
      res must not be empty

      val af = res.get
      af mustBe an[ArchiveFolder]
      af.title mustBe "archive folder 1"
      af.fid mustBe Some(folderId)
      af.owner.value.id mustBe ArchiveOwnerId(defaultMuseumId)
    }

    "update an Archive" taggedAs PG in {
      val archiveId = getArchiveId(defaultMuseumId, 0)

      val a1 =
        service.getFolder(defaultMuseumId, archiveId).futureValue.successValue.value

      val upd = a1 match {
        case ar: Archive => ar.copy(description = Some("Archive 1 updated"))
        case bad         => fail(s"Expected an Archive, got ${bad.getClass}")
      }

      service
        .updateFolder(defaultMuseumId, archiveId, upd)
        .futureValue
        .successValue mustBe Some(archiveId)

      service
        .getFolder(defaultMuseumId, archiveId)
        .futureValue
        .successValue
        .value
        .description mustBe Some("Archive 1 updated")
    }

    "update an ArchivePart" taggedAs PG in {
      val partId = getArchivePartId(defaultMuseumId, 0)

      val ap1 =
        service.getFolder(defaultMuseumId, partId).futureValue.successValue.value

      val upd = ap1 match {
        case ar: ArchivePart => ar.copy(description = Some("ArchivePart 1 updated"))
        case bad             => fail(s"Expected an Archive, got ${bad.getClass}")
      }

      service
        .updateFolder(defaultMuseumId, partId, upd)
        .futureValue
        .successValue mustBe Some(partId)

      service
        .getFolder(defaultMuseumId, partId)
        .futureValue
        .successValue
        .value
        .description mustBe Some("ArchivePart 1 updated")
    }

    "update an ArchiveFolder" taggedAs PG in {
      val folderId = getArchiveFolderId(defaultMuseumId, 0)

      val af1 =
        service.getFolder(defaultMuseumId, folderId).futureValue.successValue.value

      val upd = af1 match {
        case ar: ArchiveFolder => ar.copy(description = Some("ArchiveFolder 1 updated"))
        case bad               => fail(s"Expected an Archive, got ${bad.getClass}")
      }

      service
        .updateFolder(defaultMuseumId, folderId, upd)
        .futureValue
        .successValue mustBe Some(folderId)

      service
        .getFolder(defaultMuseumId, folderId)
        .futureValue
        .successValue
        .value
        .description mustBe Some("ArchiveFolder 1 updated")
    }

    "not rename an Archive to an already existing name" taggedAs PG in {
      val root = getRootId(defaultMuseumId)
      val archive = generateArchive(
        mid = defaultMuseumId,
        title = "archive 3",
        desc = Some("test archive 3")
      )

      failRenameFolderTest(root, archive, "archive 1")
    }

    "rename an Archive" taggedAs PG in {
      val root = getRootId(defaultMuseumId)
      val archive = generateArchive(
        mid = defaultMuseumId,
        title = "archive 4",
        desc = Some("test archive 4")
      )

      successRenameFolderTest(root, archive, "archive 4 renamed")
    }

    "not rename an ArchivePart to an already existing name" taggedAs PG in {
      val archiveId = getArchiveId(defaultMuseumId, 0)
      val part = generateArchivePart(
        mid = defaultMuseumId,
        title = "archive part 2",
        desc = Some("test archive part 2")
      )

      failRenameFolderTest(archiveId, part, "archive part 1")
    }

    "rename an ArchivePart" taggedAs PG in {
      val archiveId = getArchiveId(defaultMuseumId, 2)
      val part = generateArchivePart(
        mid = defaultMuseumId,
        title = "archive part 3",
        desc = Some("test archive part 3")
      )

      successRenameFolderTest(archiveId, part, "archive part 3 renamed")
    }

    "not rename an ArchiveFolder to an already existing name" taggedAs PG in {
      val archiveId = getArchivePartId(defaultMuseumId, 0)
      val part = generateArchiveFolder(
        mid = defaultMuseumId,
        title = "archive folder 2",
        desc = Some("test archive folder 2")
      )

      failRenameFolderTest(archiveId, part, "archive folder 1")
    }

    "rename an ArchiveFolder" taggedAs PG in {
      val archiveId = getArchivePartId(defaultMuseumId, 0)
      val part = generateArchiveFolder(
        mid = defaultMuseumId,
        title = "archive folder 3",
        desc = Some("test archive folder 3")
      )

      successRenameFolderTest(archiveId, part, "archive folder 3 renamed")
    }

    "move an ArchiveFolder" taggedAs PG in {
      val partId = getArchivePartId(defaultMuseumId, 0)
      val folder = generateArchiveFolder(
        mid = defaultMuseumId,
        title = "archive folder 4",
        desc = Some("test archive folder 4")
      )

      val aid =
        service.addFolder(defaultMuseumId, partId, folder).futureValue.successValue.value
      folderAdded(defaultMuseumId, aid, ArchiveFolder.FolderType)

      val dest = getArchiveFolderId(defaultMuseumId, 2)
      val df   = service.getFolder(defaultMuseumId, dest).futureValue.successValue.value

      val res = service.moveFolder(defaultMuseumId, aid, dest).futureValue.successValue

      res must contain(df.path.value.append(folder.title))
    }

    "not be possible to move an ArchiveFolder to an Archive" taggedAs PG in {
      val destId   = getArchiveId(defaultMuseumId, 0)
      val folderId = getArchiveFolderId(defaultMuseumId, 3)

      service.moveFolder(defaultMuseumId, folderId, destId).futureValue match {
        case MusitGeneralError(err) =>
          err must include("isn't allowed")

        case bad =>
          fail(s"Expected MusitGeneralError, got ${bad.getClass}")
      }
    }

    "not be possible to move an ArchivePart into an ArchiveFolder" taggedAs PG in {
      val destId = getArchiveFolderId(defaultMuseumId, 3)
      val partId = getArchivePartId(defaultMuseumId, 2)

      service.moveFolder(defaultMuseumId, partId, destId).futureValue match {
        case MusitGeneralError(err) =>
          err must include("isn't allowed")

        case bad =>
          fail(s"Expected MusitGeneralError, got ${bad.getClass}")
      }
    }

    "close an Archive" taggedAs PG in {
      val archiveId = getArchiveId(defaultMuseumId, 0)

      service
        .closeFolder(defaultMuseumId, archiveId)
        .futureValue
        .successValue
        .value
        .by mustBe ctx.currentUser
    }

    "not allow modifying content in a closed Archive" taggedAs PG in {
      val archiveId = getArchiveId(defaultMuseumId, 0)
      val archivePart = generateArchivePart(
        mid = defaultMuseumId,
        title = "archive part 4",
        desc = Some("test archive part 4")
      )

      service
        .addFolder(defaultMuseumId, archiveId, archivePart)
        .futureValue
        .successValue mustBe None
    }

    "reopen a closed Archive" taggedAs PG in {
      val archiveId = getArchiveId(defaultMuseumId, 0)

      service
        .reopenFolder(defaultMuseumId, archiveId)
        .futureValue
        .successValue mustBe true
    }

    "close an ArchivePart" taggedAs PG in {
      val partId = getArchivePartId(defaultMuseumId, 0)

      service
        .closeFolder(defaultMuseumId, partId)
        .futureValue
        .successValue
        .value
        .by mustBe ctx.currentUser
    }

    "not allow modifying content in a closed ArchivePart" taggedAs PG in {
      val folderId = getArchiveFolderId(defaultMuseumId, 0)

      service
        .renameFolder(defaultMuseumId, folderId, "failed name")
        .futureValue
        .successValue mustBe empty
    }

    "reopen a closed ArchivePart" taggedAs PG in {
      val partId = getArchivePartId(defaultMuseumId, 0)

      service.reopenFolder(defaultMuseumId, partId).futureValue.successValue mustBe true
    }

  }

}
