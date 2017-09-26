package controllers

import models.document.{Archive, ArchiveFolder, ArchivePart, BaseFolders}
import net.scalytica.symbiotic.api.types.Path
import net.scalytica.symbiotic.api.types.ResourceParties.Org
import no.uio.musit.models.Museums.Museum
import no.uio.musit.models.{CollectionUUID, MuseumCollections, MuseumId, Museums}
import no.uio.musit.security.BearerToken
import no.uio.musit.test.{MusitSpecWithServerPerSuite, PostgresContainer => PG}
import org.scalatest.Inspectors.forAll
import org.scalatest.time.{Millis, Span}
import play.api.libs.json._
import play.api.test.Helpers._
import utils.testdata.{ArchiveableGenerators, ArchiveableJsonGenerators, BaseDummyData}

import scala.util.Properties.envOrNone

trait DocArchUrls {

  val baseUrl = (mid: Int) => s"/museum/$mid"

  val foldersBaseUrl    = (mid: Int) => s"${baseUrl(mid)}/folders"
  val folderUrl         = (mid: Int, fid: String) => s"${foldersBaseUrl(mid)}/$fid"
  val folderMoveUrl     = (mid: Int, fid: String) => s"${folderUrl(mid, fid)}/move"
  val folderRenameUrl   = (mid: Int, fid: String) => s"${folderUrl(mid, fid)}/rename"
  val folderIsClosedUrl = (mid: Int, fid: String) => s"${folderUrl(mid, fid)}/isclosed"
  val folderCloseUrl    = (mid: Int, fid: String) => s"${folderUrl(mid, fid)}/close"
  val folderOpenUrl     = (mid: Int, fid: String) => s"${folderUrl(mid, fid)}/open"
  val folderTreeUrl     = (mid: Int, fid: String) => s"${folderUrl(mid, fid)}/tree"

  val uploadFileUrl = (mid: Int, fid: String) => s"${folderUrl(mid, fid)}/upload"

  val fileUrl         = (mid: Int, fid: String) => s"${baseUrl(mid)}/files/$fid"
  val fileDownloadUrl = (mid: Int, fid: String) => s"${fileUrl(mid, fid)}/download"
  val fileIsLockedUrl = (mid: Int, fid: String) => s"${fileUrl(mid, fid)}/islocked"
  val fileLockUrl     = (mid: Int, fid: String) => s"${fileUrl(mid, fid)}/lock"
  val fileUnlockUrl   = (mid: Int, fid: String) => s"${fileUrl(mid, fid)}/unlock"
  val fileMoveUrl     = (mid: Int, fid: String) => s"${fileUrl(mid, fid)}/move"

}

class DocumentArchiveControllerIntegrationSpec
    extends MusitSpecWithServerPerSuite
    with ArchiveSpec
    with BaseDummyData
    with ArchiveableGenerators
    with ArchiveableJsonGenerators
    with DocArchUrls {

  override val timeout =
    envOrNone("MUSIT_FUTURE_TIMEOUT").map(_.toDouble).getOrElse(5 * 1000d)
  override val interval =
    envOrNone("MUSIT_FUTURE_INTERVAL").map(_.toDouble).getOrElse(15d)

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(timeout, Millis),
    interval = Span(interval, Millis)
  )

  def validateArchiveFolderItem(
      expTitle: String,
      expDesc: Option[String],
      expOwner: MuseumId,
      expCollection: Option[CollectionUUID],
      expPath: String,
      expType: String,
      res: JsValue
  ) = {
    (res \ "id").asOpt[String] must not be empty
    (res \ "fid").asOpt[String] must not be empty
    (res \ "title").as[String] mustBe expTitle
    (res \ "description").asOpt[String] mustBe expDesc
    (res \ "owner" \ "ownerId").as[String] mustBe s"${expOwner.underlying}"
    (res \ "owner" \ "ownerType").as[String] mustBe "org"
    (res \ "collection").asOpt[String] mustBe expCollection.map(_.asString)
    (res \ "path").as[String] mustBe expPath
    (res \ "published").as[Boolean] mustBe false
    (res \ "documentMedium").as[String] mustBe "digital"
    (res \ "createdStamp" \ "date").asOpt[String] must not be empty
    (res \ "createdStamp" \ "by").asOpt[String] must not be empty
    (res \ "type").as[String] mustBe expType
  }

  def testTreeFromRootNoFiles(tok: BearerToken, mid: MuseumId) = {
    val res = wsUrl(foldersBaseUrl(mid))
      .withHttpHeaders(tok.asHeader)
      .withQueryStringParameters("includeFiles" -> "false")
      .get()
      .futureValue

    res.status mustBe OK
    res.contentType mustBe JSON

    val resArr = res.json.as[JsArray].value

    resArr.size mustBe 7

    forAll(resArr) { js =>
      (js \ "owner" \ "ownerId").as[String] mustBe mid.underlying.toString
      (js \ "owner" \ "ownerType").as[String] mustBe Org.tpe

      addFolder(mid, js)
    }

    val paths = resArr.map(js => (js \ "path").as[String]).map(Path.apply)

    val mus = Museum.fromMuseumId(mid).value

    val expectedPaths = Seq(
      Path.root,
      BaseFolders.ModulesFolderPath,
      Path.root.append(mus.shortName)
    ) ++
      BaseFolders.ModuleFolders.map(_.path(mid))

    paths must contain allElementsOf expectedPaths
  }

  def addArchiveFolderItemTest(
      tok: BearerToken,
      mid: MuseumId,
      destId: String,
      colId: Option[CollectionUUID],
      json: String
  ) = {
    val qsp = Seq.newBuilder[(String, String)]
    qsp += "destFolderId" -> destId
    colId.foreach(c => qsp += "collectionId" -> c.asString)

    val res = wsUrl(foldersBaseUrl(mid))
      .withHttpHeaders(
        CONTENT_TYPE -> JSON,
        tok.asHeader
      )
      .withQueryStringParameters(qsp.result(): _*)
      .post(json)
      .futureValue

    if (res.status == CREATED) {
      addFolder(mid, res.json)
    }

    res
  }

  "The DocumentArchiveController" when {

    "adding ArchiveFolderItems" should {

      "return the folder tree from the ArchiveRoot for the Test museum" taggedAs PG in {
        testTreeFromRootNoFiles(token, defaultMuseumId)
      }

      "return the folder tree from the ArchiveRoot for NHM" taggedAs PG in {
        testTreeFromRootNoFiles(nhmReadToken, Museums.Nhm.id)
      }

      "prevent adding an Archive without correct authorization" taggedAs PG in {
        val a      = addArchiveJsonStr("fail", Some("failed archive"))
        val rootId = getArchiveRoot(Museums.Nhm.id).fid

        addArchiveFolderItemTest(
          tokenWrite,
          Museums.Nhm.id,
          rootId,
          None,
          a
        ).status mustBe FORBIDDEN
      }

      "add an Archive to a museum" taggedAs PG in {
        val title = "foobar"
        val desc  = Some("fizz buzz description")
        val a     = addArchiveJsonStr(title, desc)
        val root  = getArchiveRoot(defaultMuseumId)

        val res = addArchiveFolderItemTest(tokenWrite, defaultMuseumId, root.fid, None, a)

        res.status mustBe CREATED
        res.contentType mustBe JSON

        validateArchiveFolderItem(
          title,
          desc,
          defaultMuseumId,
          None,
          s"/root/$title",
          Archive.FolderType,
          res.json
        )
      }

      "add an ArchivePart to an Archive" taggedAs PG in {
        val title = "brev"
        val desc  = Some("brev samlingen")
        val colId = MuseumCollections.Archeology.uuid
        val ap    = addArchivePartJsonStr(title, desc)
        val dest  = getArchive(defaultMuseumId)

        val res =
          addArchiveFolderItemTest(tokenWrite, defaultMuseumId, dest.fid, Some(colId), ap)

        res.status mustBe CREATED
        res.contentType mustBe JSON

        validateArchiveFolderItem(
          title,
          desc,
          defaultMuseumId,
          Some(colId),
          s"${dest.path}/$title",
          ArchivePart.FolderType,
          res.json
        )
      }

      "add another ArchivePart to the Archive" taggedAs PG in {
        val title = "vikinger"
        val desc  = Some("saga og dokumenter fra vikingtiden")
        val colId = MuseumCollections.Archeology.uuid
        val ap    = addArchivePartJsonStr(title, desc)
        val dest  = getArchive(defaultMuseumId)

        val res =
          addArchiveFolderItemTest(tokenWrite, defaultMuseumId, dest.fid, Some(colId), ap)

        res.status mustBe CREATED
        res.contentType mustBe JSON

        validateArchiveFolderItem(
          title,
          desc,
          defaultMuseumId,
          Some(colId),
          s"${dest.path}/$title",
          ArchivePart.FolderType,
          res.json
        )
      }

      "prevent adding an ArchivePart without correct authorization" taggedAs PG in {
        val title  = "tullball"
        val desc   = None
        val colId  = MuseumCollections.Archeology.uuid
        val ap     = addArchivePartJsonStr(title, desc)
        val destId = getArchive(defaultMuseumId).fid

        addArchiveFolderItemTest(
          nhmReadToken,
          defaultMuseumId,
          destId,
          Some(colId),
          ap
        ).status mustBe FORBIDDEN
      }

      "prevent adding an ArchivePart to the root" taggedAs PG in {
        val title  = "tullball"
        val desc   = None
        val colId  = MuseumCollections.Archeology.uuid
        val ap     = addArchivePartJsonStr(title, desc)
        val destId = getArchiveRoot(defaultMuseumId).fid

        val res = addArchiveFolderItemTest(
          tokenWrite,
          defaultMuseumId,
          destId,
          Some(colId),
          ap
        )
        res.status mustBe BAD_REQUEST
        res.contentType mustBe JSON
        (res.json \ "message").as[String] must include("invalid location")
      }

      "add an ArchiveFolder to an ArchivePart" taggedAs PG in {
        val title = "Reisebrev"
        val desc  = Some("Brev fra forskjellige reiser rundt i verden")
        val colId = MuseumCollections.Archeology.uuid
        val af    = addArchiveFolderJsonStr(title, desc)
        val dest  = getArchiveParts(defaultMuseumId).headOption.value

        val res =
          addArchiveFolderItemTest(tokenWrite, defaultMuseumId, dest.fid, Some(colId), af)

        res.status mustBe CREATED
        res.contentType mustBe JSON

        validateArchiveFolderItem(
          title,
          desc,
          defaultMuseumId,
          Some(colId),
          s"${dest.path}/$title",
          ArchiveFolder.FolderType,
          res.json
        )
      }

      "add another ArchiveFolder to an ArchivePart" taggedAs PG in {
        val title = "Kong Haakons korrespondanse"
        val desc  = Some("Brev til folket")
        val colId = MuseumCollections.Archeology.uuid
        val af    = addArchiveFolderJsonStr(title, desc)
        val dest  = getArchiveParts(defaultMuseumId).headOption.value

        val res =
          addArchiveFolderItemTest(tokenWrite, defaultMuseumId, dest.fid, Some(colId), af)

        res.status mustBe CREATED
        res.contentType mustBe JSON

        validateArchiveFolderItem(
          title,
          desc,
          defaultMuseumId,
          Some(colId),
          s"${dest.path}/$title",
          ArchiveFolder.FolderType,
          res.json
        )
      }

      "prevent adding an ArchiveFolder without correct authorization" taggedAs PG in {
        val title  = "tullball"
        val desc   = None
        val colId  = MuseumCollections.Archeology.uuid
        val ap     = addArchiveFolderJsonStr(title, desc)
        val destId = getArchiveFolders(defaultMuseumId).lastOption.value.fid

        addArchiveFolderItemTest(
          nhmReadToken,
          defaultMuseumId,
          destId,
          Some(colId),
          ap
        ).status mustBe FORBIDDEN
      }

      "prevent adding an ArchiveFolder to the root" taggedAs PG in {
        val title  = "tullball"
        val desc   = None
        val colId  = MuseumCollections.Archeology.uuid
        val ap     = addArchiveFolderJsonStr(title, desc)
        val destId = getArchiveRoot(defaultMuseumId).fid

        val res = addArchiveFolderItemTest(
          tokenWrite,
          defaultMuseumId,
          destId,
          Some(colId),
          ap
        )
        res.status mustBe BAD_REQUEST
        res.contentType mustBe JSON
        (res.json \ "message").as[String] must include("invalid location")
      }

      "add an ArchiveFolder to another ArchiveFolder" taggedAs PG in {
        val title = "29 september 1789"
        val desc  = None
        val colId = MuseumCollections.Archeology.uuid
        val af    = addArchiveFolderJsonStr(title, desc)
        val dest  = getArchiveFolders(defaultMuseumId).lastOption.value

        val res =
          addArchiveFolderItemTest(tokenWrite, defaultMuseumId, dest.fid, Some(colId), af)

        res.status mustBe CREATED
        res.contentType mustBe JSON

        validateArchiveFolderItem(
          title,
          desc,
          defaultMuseumId,
          Some(colId),
          s"${dest.path}/$title",
          ArchiveFolder.FolderType,
          res.json
        )
      }
    }

    "working with existing ArchiveFolderItems" should {
      "update metadata" taggedAs PG in {
        pending
      }

      "rename an ArchiveFolderItem" taggedAs PG in {
        pending
      }

      "moving ..." taggedAs PG in {
        pending
      }

      "isclosed" taggedAs PG in {
        pending
      }

      "close folder" taggedAs PG in {
        pending
      }

      "open folder" taggedAs PG in {
        pending
      }

      "children" taggedAs PG in {
        pending
      }

      "tree" taggedAs PG in {
        pending
      }

    }

    "adding ArchiveDocuments" should {
      "upload file" taggedAs PG in {
        pending
      }

      "update file" taggedAs PG in {
        pending
      }

      "get file metadata" taggedAs PG in {
        pending
      }

      "download file" taggedAs PG in {
        pending
      }

      "is locked" taggedAs PG in {
        pending
      }

      "lock" taggedAs PG in {
        pending
      }

      "unlock" taggedAs PG in {
        pending
      }

      "move" taggedAs PG in {
        pending
      }
    }

  }

}
