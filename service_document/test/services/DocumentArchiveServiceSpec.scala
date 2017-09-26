package services

import models.document.ArchiveIdentifiers.ArchiveOwnerId
import models.document.Implicits._
import models.document.{ArchiveAddContext, ArchiveContext, _}
import modules.Bootstrapper
import net.scalytica.symbiotic.api.types.{FileId, FolderId}
import net.scalytica.symbiotic.test.specs.PostgresSpec
import no.uio.musit.MusitResults.MusitGeneralError
import no.uio.musit.models.{MuseumCollections, MuseumId, Museums}
import no.uio.musit.test.matchers.MusitResultValues
import no.uio.musit.test.{MusitSpecWithAppPerSuite, PostgresContainer => PG}
import org.scalatest.Inspectors.forAll
import org.scalatest.time.{Millis, Span}
import utils.testdata.{ArchiveableGenerators, BaseDummyData}

import scala.concurrent.Await
import scala.concurrent.duration._
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
  private[this] val files   = List.newBuilder[(MuseumId, FileId)]

  private[this] def folderAdded(mid: MuseumId, fid: FolderId, tpe: String): Unit = {
    folders += ((mid, fid, tpe))
  }

  private[this] def fileAdded(mid: MuseumId, fid: FileId): Unit = files += ((mid, fid))

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

  private[this] def getArchiveDocumentId(mid: MuseumId, idx: Int): FileId = {
    files.result().filter(_._1 == mid)(idx)._2
  }

  override def initDatabase(): Either[String, Unit] = {
    val res = super.initDatabase()

    Await.result(Bootstrapper.init(service.dmService), 5 seconds)

    res
  }

  def failRenameFolderTest[A <: ArchiveFolderItem](
      parentId: FolderId,
      afi: A,
      existingFolderName: String
  ) = {
    val aid =
      service.addArchiveFolderItem(parentId, afi).futureValue.successValue.fid.value
    folderAdded(defaultMuseumId, aid, afi.typeString)

    service.renameArchiveFolderItem(aid, existingFolderName).futureValue match {
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
      service.addArchiveFolderItem(parentId, afi).futureValue.successValue.fid.value
    folderAdded(defaultMuseumId, aid, afi.typeString)

    val ap = service.getArchiveFolderItem(aid).futureValue.successValue

    val res = service.renameArchiveFolderItem(aid, name).futureValue.successValue

    res must contain(ap.path.value.parent.append(name))
  }

  "The DocumentArchiveService" should {

    "get the root folder for the Test museum" taggedAs PG in {
      val res = service.archiveRoot(defaultMuseumId).futureValue.successValue
      res.title mustBe "root"
      res.metadata.fid must not be empty
      folderAdded(defaultMuseumId, res.metadata.fid.value, ArchiveRoot.FolderType)
    }

    "add an Archive folder to the root" taggedAs PG in {
      val root = getRootId(defaultMuseumId)
      val archive = generateArchive(
        title = "archive 1",
        desc = Some("test archive 1")
      )

      val res = service.addArchiveFolderItem(root, archive).futureValue.successValue

      folderAdded(defaultMuseumId, res.fid.value, Archive.FolderType)
    }

    "not allow adding an Archive to an Archive" taggedAs PG in {
      val archiveId = getArchiveId(defaultMuseumId, 0)
      val archive   = generateArchive(title = "archive_fail")

      service.addArchiveFolderItem(archiveId, archive).futureValue match {
        case MusitGeneralError(err) =>
          err must include("invalid location")

        case bad => fail(s"Expected MusitGeneralError but got ${bad.getClass}")
      }
    }

    "not allow adding an ArchivePart folder to the root" taggedAs PG in {
      val root = getRootId(defaultMuseumId)
      val archivePart = generateArchivePart(
        title = "archive part",
        desc = Some("test archive part")
      )

      service.addArchiveFolderItem(root, archivePart).futureValue match {
        case MusitGeneralError(err) =>
          err must include("invalid location")

        case bad => fail(s"Expected MusitGeneralError but got ${bad.getClass}")
      }
    }

    "not allow adding an ArchiveFolder to the root" taggedAs PG in {
      val root = getRootId(defaultMuseumId)
      val archiveFolder = generateArchiveFolder(
        title = "archive folder",
        desc = Some("test archive folder")
      )

      service.addArchiveFolderItem(root, archiveFolder).futureValue match {
        case MusitGeneralError(err) =>
          err must include("invalid location")

        case bad => fail(s"Expected MusitGeneralError but got ${bad.getClass}")
      }
    }

    "add an ArchivePart to an Archive" taggedAs PG in {
      val archiveId = getArchiveId(defaultMuseumId, 0)
      val archivePart = generateArchivePart(
        title = "archive part 1",
        desc = Some("test archive part 1")
      )

      val res =
        service.addArchiveFolderItem(archiveId, archivePart).futureValue.successValue

      folderAdded(defaultMuseumId, res.fid.value, ArchivePart.FolderType)
    }

    "not allow adding an Archive to an ArchivePart" taggedAs PG in {
      val partId  = getArchivePartId(defaultMuseumId, 0)
      val archive = generateArchive(title = "archive_fail")

      service.addArchiveFolderItem(partId, archive).futureValue match {
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
      val folder = generateArchiveFolder(title = "archive folder 1")

      val res =
        service.addArchiveFolderItem(partId, folder).futureValue.successValue

      folderAdded(defaultMuseumId, res.fid.value, ArchiveFolder.FolderType)
    }

    "not allow adding an Archive to an ArchiveFolder" taggedAs PG in {
      val folderId = getArchiveFolderId(defaultMuseumId, 0)
      val archive  = generateArchive(title = "archive_fail")

      service.addArchiveFolderItem(folderId, archive).futureValue match {
        case MusitGeneralError(err) =>
          err must include("invalid location")

        case bad => fail(s"Expected MusitGeneralError but got ${bad.getClass}")
      }
    }

    "not allow adding an ArchivePart to an ArchiveFolder" taggedAs PG in {
      val folderId = getArchiveFolderId(defaultMuseumId, 0)
      val part     = generateArchivePart(title = "archive_part_fail")

      service.addArchiveFolderItem(folderId, part).futureValue match {
        case MusitGeneralError(err) =>
          err must include("invalid location")

        case bad => fail(s"Expected MusitGeneralError but got ${bad.getClass}")
      }
    }

    "add an ArchiveFolder to another ArchiveFolder" taggedAs PG in {
      val folderId = getArchiveFolderId(defaultMuseumId, 0)
      val folder   = generateArchiveFolder(title = "archive folder 2")

      val res =
        service.addArchiveFolderItem(folderId, folder).futureValue.successValue

      folderAdded(defaultMuseumId, res.fid.value, ArchiveFolder.FolderType)
    }

    "not add an ArchiveFolder with name of existing ArchiveFolder" taggedAs PG in {
      val folderId = getArchiveFolderId(defaultMuseumId, 0)
      val folder   = generateArchiveFolder(title = "archive folder 2")

      service.addArchiveFolderItem(folderId, folder).futureValue match {
        case MusitGeneralError(err) =>
          err must include("already exists")

        case bad =>
          fail(s"Expected MusitGeneralError but got ${bad.getClass}")
      }
    }

    "get an Archive by id" taggedAs PG in {
      val archiveId = getArchiveId(defaultMuseumId, 0)

      val res = service.getArchiveFolderItem(archiveId).futureValue.successValue

      res mustBe an[Archive]
      res.title mustBe "archive 1"
      res.fid mustBe Some(archiveId)
      res.owner.value.id mustBe ArchiveOwnerId(defaultMuseumId)
    }

    "get an ArchivePart by id" taggedAs PG in {
      val partId = getArchivePartId(defaultMuseumId, 0)

      val res = service.getArchiveFolderItem(partId).futureValue.successValue

      res mustBe an[ArchivePart]
      res.title mustBe "archive part 1"
      res.fid mustBe Some(partId)
      res.owner.value.id mustBe ArchiveOwnerId(defaultMuseumId)
    }

    "get an ArchiveFolder by id" taggedAs PG in {
      val folderId = getArchiveFolderId(defaultMuseumId, 0)

      val res = service.getArchiveFolderItem(folderId).futureValue.successValue

      res mustBe an[ArchiveFolder]
      res.title mustBe "archive folder 1"
      res.fid mustBe Some(folderId)
      res.owner.value.id mustBe ArchiveOwnerId(defaultMuseumId)
    }

    "update an Archive" taggedAs PG in {
      val archiveId = getArchiveId(defaultMuseumId, 0)

      val a1 = service.getArchiveFolderItem(archiveId).futureValue.successValue

      val upd = a1 match {
        case ar: Archive => ar.copy(description = Some("Archive 1 updated"))
        case bad         => fail(s"Expected an Archive, got ${bad.getClass}")
      }

      service
        .updateArchiveFolderItem(archiveId, upd)
        .futureValue
        .successValue
        .description mustBe Some("Archive 1 updated")
    }

    "update an ArchivePart" taggedAs PG in {
      val partId = getArchivePartId(defaultMuseumId, 0)

      val ap1 = service.getArchiveFolderItem(partId).futureValue.successValue

      val upd = ap1 match {
        case ar: ArchivePart => ar.copy(description = Some("ArchivePart 1 updated"))
        case bad             => fail(s"Expected an Archive, got ${bad.getClass}")
      }

      service
        .updateArchiveFolderItem(partId, upd)
        .futureValue
        .successValue
        .description mustBe Some("ArchivePart 1 updated")
    }

    "update an ArchiveFolder" taggedAs PG in {
      val folderId = getArchiveFolderId(defaultMuseumId, 0)

      val af1 = service.getArchiveFolderItem(folderId).futureValue.successValue

      val upd = af1 match {
        case ar: ArchiveFolder => ar.copy(description = Some("ArchiveFolder 1 updated"))
        case bad               => fail(s"Expected an Archive, got ${bad.getClass}")
      }

      service
        .updateArchiveFolderItem(folderId, upd)
        .futureValue
        .successValue
        .description mustBe Some("ArchiveFolder 1 updated")
    }

    "not rename an Archive to an already existing name" taggedAs PG in {
      val root = getRootId(defaultMuseumId)
      val archive = generateArchive(
        title = "archive 3",
        desc = Some("test archive 3")
      )

      failRenameFolderTest(root, archive, "archive 1")
    }

    "rename an Archive" taggedAs PG in {
      val root = getRootId(defaultMuseumId)
      val archive = generateArchive(
        title = "archive 4",
        desc = Some("test archive 4")
      )

      successRenameFolderTest(root, archive, "archive 4 renamed")
    }

    "not rename an ArchivePart to an already existing name" taggedAs PG in {
      val archiveId = getArchiveId(defaultMuseumId, 0)
      val part = generateArchivePart(
        title = "archive part 2",
        desc = Some("test archive part 2")
      )

      failRenameFolderTest(archiveId, part, "archive part 1")
    }

    "rename an ArchivePart" taggedAs PG in {
      val archiveId = getArchiveId(defaultMuseumId, 2)
      val part = generateArchivePart(
        title = "archive part 3",
        desc = Some("test archive part 3")
      )

      successRenameFolderTest(archiveId, part, "archive part 3 renamed")
    }

    "not rename an ArchiveFolder to an already existing name" taggedAs PG in {
      val archiveId = getArchivePartId(defaultMuseumId, 0)
      val part = generateArchiveFolder(
        title = "archive folder 3",
        desc = Some("test archive folder 3")
      )

      failRenameFolderTest(archiveId, part, "archive folder 1")
    }

    "rename an ArchiveFolder" taggedAs PG in {
      val archiveId = getArchivePartId(defaultMuseumId, 0)
      val part = generateArchiveFolder(
        title = "archive folder 4",
        desc = Some("test archive folder 4")
      )

      successRenameFolderTest(archiveId, part, "archive folder 4 renamed")
    }

    "move an ArchiveFolder" taggedAs PG in {
      val partId = getArchivePartId(defaultMuseumId, 0)
      val folder = generateArchiveFolder(
        title = "archive folder 5",
        desc = Some("test archive folder 5")
      )

      val aid =
        service.addArchiveFolderItem(partId, folder).futureValue.successValue.fid.value
      folderAdded(defaultMuseumId, aid, ArchiveFolder.FolderType)

      val dest = getArchiveFolderId(defaultMuseumId, 2)
      val df   = service.getArchiveFolderItem(dest).futureValue.successValue

      service.moveArchiveFolderItem(aid, dest).futureValue.successValue must contain(
        df.path.value.append(folder.title)
      )
    }

    "not be possible to move an ArchiveFolder to an Archive" taggedAs PG in {
      val destId   = getArchiveId(defaultMuseumId, 0)
      val folderId = getArchiveFolderId(defaultMuseumId, 3)

      service.moveArchiveFolderItem(folderId, destId).futureValue match {
        case MusitGeneralError(err) =>
          err must include("isn't allowed")

        case bad =>
          fail(s"Expected MusitGeneralError, got ${bad.getClass}")
      }
    }

    "not be possible to move an ArchivePart into an ArchiveFolder" taggedAs PG in {
      val destId = getArchiveFolderId(defaultMuseumId, 3)
      val partId = getArchivePartId(defaultMuseumId, 2)

      service.moveArchiveFolderItem(partId, destId).futureValue match {
        case MusitGeneralError(err) =>
          err must include("isn't allowed")

        case bad =>
          fail(s"Expected MusitGeneralError, got ${bad.getClass}")
      }
    }

    "close an Archive" taggedAs PG in {
      val archiveId = getArchiveId(defaultMuseumId, 0)

      service
        .closeArchiveFolderItem(archiveId)
        .futureValue
        .successValue
        .by mustBe ctx.currentUser
    }

    "not allow modifying content in a closed Archive" taggedAs PG in {
      val archiveId = getArchiveId(defaultMuseumId, 0)
      val archivePart = generateArchivePart(
        title = "archive part 4",
        desc = Some("test archive part 4")
      )

      service.addArchiveFolderItem(archiveId, archivePart).futureValue match {
        case MusitGeneralError(msg) =>
          msg must include("was not created")

        case wrong =>
          fail(s"Expected MusitGeneralError, got ${wrong.getClass}")
      }
    }

    "reopen a closed Archive" taggedAs PG in {
      val archiveId = getArchiveId(defaultMuseumId, 0)

      service.openArchiveFolderItem(archiveId).futureValue.successValue mustBe true
    }

    "close an ArchivePart" taggedAs PG in {
      val partId = getArchivePartId(defaultMuseumId, 0)

      service
        .closeArchiveFolderItem(partId)
        .futureValue
        .successValue
        .by mustBe ctx.currentUser
    }

    "not allow modifying content in a closed ArchivePart" taggedAs PG in {
      val folderId = getArchiveFolderId(defaultMuseumId, 0)

      service.renameArchiveFolderItem(folderId, "failed name").futureValue match {
        case MusitGeneralError(msg) =>
          msg must include("Couldn't change name")

        case wrong =>
          fail(s"Expected MusitGeneralError, got ${wrong.getClass}")
      }
    }

    "reopen a closed ArchivePart" taggedAs PG in {
      val partId = getArchivePartId(defaultMuseumId, 0)

      service.openArchiveFolderItem(partId).futureValue.successValue mustBe true
    }

    "save an ArchiveDocument in an ArchiveFolder" taggedAs PG in {
      val folderId = getArchiveFolderId(defaultMuseumId, 0)
      val ad = generateArchiveDocument(
        author = "Darth Vader",
        title = "archive document 1",
        desc = Some("test archive document 1")
      )

      val res = service.saveArchiveDocument(folderId, ad).futureValue.successValue

      fileAdded(defaultMuseumId, res)
    }

    "save an ArchiveDocument in an ArchivePart" taggedAs PG in {
      val partId = getArchivePartId(defaultMuseumId, 0)
      val ad = generateArchiveDocument(
        author = "Darth Vader",
        title = "archive document 2",
        desc = Some("test archive document 2")
      )

      val res = service.saveArchiveDocument(partId, ad).futureValue.successValue

      fileAdded(defaultMuseumId, res)
    }

    "save an ArchiveDocument in an Archive" taggedAs PG in {
      val archiveId = getArchiveId(defaultMuseumId, 0)
      val ad = generateArchiveDocument(
        author = "Darth Vader",
        title = "archive document 3",
        desc = Some("test archive document 3")
      )

      val res = service.saveArchiveDocument(archiveId, ad).futureValue.successValue

      fileAdded(defaultMuseumId, res)
    }

    "get an ArchiveDocument" taggedAs PG in {
      val fid = getArchiveDocumentId(defaultMuseumId, 0)

      val res = service.getArchiveDocument(fid).futureValue.successValue

      res.title mustBe "archive document 1"
      res.description mustBe Some("test archive document 1")
      res.author mustBe Some("Darth Vader")

      res.stream must not be empty
    }

    "update an ArchiveDocument" taggedAs PG in {
      val folderId = getArchiveFolderId(defaultMuseumId, 0)
      val ad = generateArchiveDocument(
        author = "Darth Vader",
        title = "archive document 4",
        desc = Some("test archive document 4")
      )

      val fid = service.saveArchiveDocument(folderId, ad).futureValue.successValue
      fileAdded(defaultMuseumId, fid)

      val orig = service.getArchiveDocument(fid).futureValue.successValue

      val mod = orig.copy(
        description = Some("test archive document 4 - updated"),
        documentMedium = Some("paper"),
        author = Some("Anakin Skywalker")
      )

      val res = service.updateArchiveDocument(fid, mod).futureValue.successValue

      res.title mustBe orig.title
      res.author mustBe Some("Anakin Skywalker")
      res.description mustBe Some("test archive document 4 - updated")
      res.documentMedium mustBe Some("paper")
      res.path mustBe orig.path
    }

    "update an ArchiveDocument except immutable fields" taggedAs PG in {
      val fid = getArchiveDocumentId(defaultMuseumId, 3)

      val orig = service.getArchiveDocument(fid).futureValue.successValue

      val mod = orig.copy(
        title = "foo",
        version = orig.version + 10,
        fileType = Some("blæh!")
      )

      val res = service.updateArchiveDocument(fid, mod).futureValue.successValue

      res.fid mustBe orig.fid
      res.title mustBe orig.title
      res.version mustBe orig.version + 1
      res.fileType mustBe orig.fileType
    }

    /*
      Tree now looks as follows

      FOLDERS:
      (archive 1               , Some(Path(/root/archive 1)))
      (archive part 1          , Some(Path(/root/archive 1/archive part 1)))
      (archive folder 1        , Some(Path(/root/archive 1/archive part 1/archive folder 1)))
      (archive folder 2        , Some(Path(/root/archive 1/archive part 1/archive folder 1/archive folder 2)))
      (archive folder 3        , Some(Path(/root/archive 1/archive part 1/archive folder 3)))
      (archive folder 5        , Some(Path(/root/archive 1/archive part 1/archive folder 3/archive folder 5)))
      (archive folder 4 renamed, Some(Path(/root/archive 1/archive part 1/archive folder 4 renamed)))
      (archive part 2          , Some(Path(/root/archive 1/archive part 2)))

      FILES:
      (archive document 3      , Some(Path(/root/archive 1)))
      (archive document 2      , Some(Path(/root/archive 1/archive part 1)))
      (archive document 1      , Some(Path(/root/archive 1/archive part 1/archive folder 1)))
      (archive document 4      , Some(Path(/root/archive 1/archive part 1/archive folder 1)))
     */

    "return an entire Archive tree" taggedAs PG in {
      val archiveId = getArchiveId(defaultMuseumId, 0)

      val res =
        service.getTreeFrom(archiveId, includeFiles = true).futureValue.successValue

      val archives = res.filter(_.isInstanceOf[Archive])
      val parts    = res.filter(_.isInstanceOf[ArchivePart])
      val folders  = res.filter(_.isInstanceOf[ArchiveFolder])
      val docs     = res.filter(_.isInstanceOf[ArchiveDocument])

      archives.size mustBe 1
      parts.size mustBe 2
      folders.size mustBe 5
      docs.size mustBe 4

      forAll(archives)(a => a.title must startWith("archive "))
      forAll(parts)(p => p.title must startWith("archive part "))
      forAll(folders)(f => f.title must startWith("archive folder "))
      forAll(docs)(doc => doc.title must startWith("archive document "))
    }

    "return all direct children of an ArchiveFolderItem" taggedAs PG in {
      val partId = getArchivePartId(defaultMuseumId, 0)
      val expected = Seq(
        "archive folder 1",
        "archive folder 3",
        "archive folder 4 renamed",
        "archive document 2"
      )

      val res = service.getChildrenFor(partId).futureValue.successValue

      res.size mustBe 4
      res.map(_.title) must contain allElementsOf expected
    }

  }

}
