package controllers

import models.document.{Archive, ArchiveFolder, ArchivePart, BaseFolders}
import net.scalytica.symbiotic.api.types.Path
import net.scalytica.symbiotic.api.types.ResourceParties.Org
import no.uio.musit.models.Museums.Museum
import no.uio.musit.models.{CollectionUUID, MuseumCollections, MuseumId, Museums}
import no.uio.musit.security.BearerToken
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite, PostgresContainer => PG}
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
  val folderChildrenUrl = (mid: Int, fid: String) => s"${folderUrl(mid, fid)}/children"
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

  def httpPostFolderTest(
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

  def httpPutFolderTest(
      tok: BearerToken,
      url: String,
      mid: MuseumId = defaultMuseumId,
      maybeJson: Option[String] = None,
      queryParams: Seq[(String, String)] = Seq.empty,
      contentType: String = JSON
  ) = {
    val req = wsUrl(url)
      .withHttpHeaders(
        CONTENT_TYPE -> contentType,
        tok.asHeader
      )
      .withQueryStringParameters(queryParams: _*)

    maybeJson.map(js => req.put(js)).getOrElse(req.execute(PUT)).futureValue
  }

  def httpGetTest(
      tok: BearerToken,
      url: String,
      mid: MuseumId = defaultMuseumId,
      queryParams: Seq[(String, String)] = Seq.empty,
      contentType: String = JSON
  ) = {
    wsUrl(url)
      .withHttpHeaders(
        CONTENT_TYPE -> contentType,
        tok.asHeader
      )
      .withQueryStringParameters(queryParams: _*)
      .get()
      .futureValue
  }

  "The DocumentArchiveController" when {

    "adding ArchiveFolderItems" should {

      "return the folder tree from the ArchiveRoot for the Test museum" taggedAs PG in {
        testTreeFromRootNoFiles(tokenAdmin, defaultMuseumId)
      }

      "return the folder tree from the ArchiveRoot for NHM" taggedAs PG in {
        testTreeFromRootNoFiles(nhmReadToken, Museums.Nhm.id)
      }

      "prevent adding an Archive without correct authorization" taggedAs PG in {
        val a      = addArchiveJsonStr("fail", Some("failed archive"))
        val rootId = getArchiveRoot(Museums.Nhm.id).fid

        httpPostFolderTest(
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

        val res = httpPostFolderTest(tokenWrite, defaultMuseumId, root.fid, None, a)

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
          httpPostFolderTest(tokenWrite, defaultMuseumId, dest.fid, Some(colId), ap)

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
          httpPostFolderTest(tokenWrite, defaultMuseumId, dest.fid, Some(colId), ap)

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

        httpPostFolderTest(
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

        val res = httpPostFolderTest(
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
          httpPostFolderTest(tokenWrite, defaultMuseumId, dest.fid, Some(colId), af)

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
          httpPostFolderTest(tokenWrite, defaultMuseumId, dest.fid, Some(colId), af)

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

        httpPostFolderTest(
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

        val res = httpPostFolderTest(
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
          httpPostFolderTest(tokenWrite, defaultMuseumId, dest.fid, Some(colId), af)

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

      "add yet another ArchiveFolder to the second ArchivePart" taggedAs PG in {
        val title = "Snorre"
        val desc  = Some("Manuskript fra Snorre")
        val colId = MuseumCollections.Archeology.uuid
        val af    = addArchiveFolderJsonStr(title, desc)
        val dest  = getArchiveParts(defaultMuseumId).lastOption.value

        val res =
          httpPostFolderTest(tokenWrite, defaultMuseumId, dest.fid, Some(colId), af)

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

      "confirm that an ArchiveFolderItem isn't closed" taggedAs PG in {
        val fid = getArchiveFolders(defaultMuseumId).headOption.value.fid
        val res = httpGetTest(
          tokenAdmin,
          folderIsClosedUrl(defaultMuseumId, fid)
        )

        res.status mustBe OK
        res.contentType mustBe JSON
        (res.json \ "isLocked").as[Boolean] mustBe false
      }

      "prevent unauthorized closing of an ArchiveFolderItem" taggedAs PG in {
        val fid = getArchiveParts(defaultMuseumId).lastOption.value.fid

        httpPutFolderTest(
          nhmReadToken,
          folderCloseUrl(defaultMuseumId, fid)
        ).status mustBe FORBIDDEN
      }

      "close an ArchiveFolderItem" taggedAs PG in {
        val fid = getArchiveParts(defaultMuseumId).lastOption.value.fid
        val res = httpPutFolderTest(
          tokenAdmin,
          folderCloseUrl(defaultMuseumId, fid)
        )

        res.status mustBe OK
        res.contentType mustBe JSON

        (res.json \ "by").as[String] mustBe FakeUsers.testAdminId
        (res.json \ "date").asOpt[String] must not be empty
      }

      "confirm that an ArchiveFolderItem is closed" taggedAs PG in {
        val fid = getArchiveParts(defaultMuseumId).lastOption.value.fid
        val res = httpGetTest(
          tokenAdmin,
          folderIsClosedUrl(defaultMuseumId, fid)
        )

        res.status mustBe OK
        res.contentType mustBe JSON
        (res.json \ "isLocked").as[Boolean] mustBe true
      }

      "prevent unauthorized update of metadata on an ArchiveFolderItem" taggedAs PG in {
        val fid = getArchiveFolders(defaultMuseumId).headOption.value.fid

        val orig = httpGetTest(
          tokenRead,
          folderUrl(defaultMuseumId, fid)
        )
        orig.status mustBe OK
        orig.contentType mustBe JSON

        val updateJs = orig.json.as[JsObject] ++ Json.obj(
          "description"    -> s"${(orig.json \ "description").as[String]} updated",
          "documentMedium" -> s"skuff"
        )

        httpPutFolderTest(
          tok = nhmReadToken,
          url = folderUrl(defaultMuseumId, fid),
          maybeJson = Some(Json.stringify(updateJs))
        ).status mustBe FORBIDDEN
      }

      "update metadata values on an ArchiveFolderItem" taggedAs PG in {
        val fid = getArchiveFolders(defaultMuseumId).headOption.value.fid

        val orig = httpGetTest(
          tokenAdmin,
          folderUrl(defaultMuseumId, fid)
        )
        orig.status mustBe OK
        orig.contentType mustBe JSON

        val updateJs = orig.json.as[JsObject] ++ Json.obj(
          "description"    -> s"${(orig.json \ "description").as[String]} updated",
          "documentMedium" -> s"skuff"
        )

        val updRes = httpPutFolderTest(
          tok = tokenWrite,
          url = folderUrl(defaultMuseumId, fid),
          maybeJson = Some(Json.stringify(updateJs))
        )

        updRes.status mustBe OK
        updRes.contentType mustBe JSON

        (updRes.json \ "fid").as[String] mustBe fid
        (updRes.json \ "title").as[String] mustBe (orig.json \ "title").as[String]
        (updRes.json \ "description").as[String] must include("updated")
        (updRes.json \ "documentMedium").as[String] mustBe "skuff"
      }

      "prevent unauthorized renaming of an ArchiveFolderItem" taggedAs PG in {
        val fid = getArchiveFolders(defaultMuseumId).headOption.value.fid

        httpPutFolderTest(
          tok = nhmReadToken,
          url = folderRenameUrl(defaultMuseumId, fid),
          queryParams = Seq("name" -> "should fail")
        ).status mustBe FORBIDDEN
      }

      "prevent renaming an ArchiveFolder where a parent is closed" taggedAs PG in {
        val fid = getArchiveParts(defaultMuseumId).lastOption.value.fid

        httpPutFolderTest(
          nhmReadToken,
          folderRenameUrl(defaultMuseumId, fid),
          queryParams = Seq("name" -> "should fail")
        ).status mustBe FORBIDDEN
      }

      "rename an ArchiveFolderItem" taggedAs PG in {
        val fid      = getArchiveFolders(defaultMuseumId).headOption.value.fid
        val newTitle = "Reisebrev fra historiske personer"

        val res = httpPutFolderTest(
          tok = tokenWrite,
          url = folderRenameUrl(defaultMuseumId, fid),
          queryParams = Seq("name" -> newTitle)
        )

        res.status mustBe OK
        res.contentType mustBe JSON

        val jsArr = res.json.as[JsArray].value
        jsArr.size mustBe 1
        jsArr.headOption.value.as[String] mustBe s"/root/Test/brev/$newTitle"
      }

      "prevent moving an ArchiveFolder where a parent is closed" taggedAs PG in {
        val srcFid = getArchiveFolders(defaultMuseumId).lastOption.value.fid
        val dstFid = getArchiveParts(defaultMuseumId).headOption.value.fid

        val res = httpPutFolderTest(
          tokenWrite,
          folderMoveUrl(defaultMuseumId, srcFid),
          queryParams = Seq("to" -> dstFid)
        )

        res.status mustBe BAD_REQUEST
        res.contentType mustBe JSON

        (res.json \ "message").as[String] must include("was not moved")
      }

      "prevent unauthorized move of an ArchiveFolder" taggedAs PG in {
        val srcFid = getArchiveFolders(defaultMuseumId).lastOption.value.fid
        val dstFid = getArchiveParts(defaultMuseumId).headOption.value.fid

        httpPutFolderTest(
          tokenRead,
          folderMoveUrl(defaultMuseumId, srcFid),
          queryParams = Seq("to" -> dstFid)
        ).status mustBe FORBIDDEN
      }

      "move an ArchiveFolder to another ArchivePart" taggedAs PG in {
        val src      = getArchiveFolders(defaultMuseumId)(2)
        val dst      = getArchiveParts(defaultMuseumId).headOption.value
        val expTitle = "29 september 1789"

        val res = httpPutFolderTest(
          tokenWrite,
          folderMoveUrl(defaultMuseumId, src.fid),
          queryParams = Seq("to" -> dst.fid)
        )

        res.status mustBe OK
        res.contentType mustBe JSON

        val jsArr = res.json.as[JsArray].value
        jsArr.size mustBe 1
        jsArr.headOption.value.as[String] mustBe s"${dst.path}/$expTitle"
      }

      "move an ArchiveFolder to another ArchiveFolder" taggedAs PG in {
        // Basically moving back to its original destination, so expected
        // path is the same as the original for src
        val src      = getArchiveFolders(defaultMuseumId)(2)
        val dst      = getArchiveFolders(defaultMuseumId)(1)
        val expTitle = "29 september 1789"

        val res = httpPutFolderTest(
          tokenWrite,
          folderMoveUrl(defaultMuseumId, src.fid),
          queryParams = Seq("to" -> dst.fid)
        )

        res.status mustBe OK
        res.contentType mustBe JSON

        val jsArr = res.json.as[JsArray].value
        jsArr.size mustBe 1
        jsArr.headOption.value.as[String] mustBe src.path
      }

      "prevent unauthorized opening a closed ArchiveFolderItem" taggedAs PG in {
        val fid = getArchiveParts(defaultMuseumId).lastOption.value.fid
        httpPutFolderTest(
          tokenRead,
          folderOpenUrl(defaultMuseumId, fid)
        ).status mustBe FORBIDDEN
      }

      "open a previously closed ArchiveFolderItem" taggedAs PG in {
        val fid = getArchiveParts(defaultMuseumId).lastOption.value.fid
        val res = httpPutFolderTest(
          tokenAdmin,
          folderOpenUrl(defaultMuseumId, fid)
        )

        res.status mustBe OK
      }

      "return a list with the direct children for an ArchiveFolderItem" taggedAs PG in {
        val folder = getArchiveParts(defaultMuseumId).headOption.value
        val expTitles = Seq(
          "Kong Haakons korrespondanse",
          "Reisebrev fra historiske personer"
        )

        val res = httpGetTest(
          tokenRead,
          folderChildrenUrl(defaultMuseumId, folder.fid)
        )

        res.status mustBe OK
        res.contentType mustBe JSON

        val jsArr = res.json.as[JsArray].value
        jsArr.size mustBe 2
        jsArr.map(js => (js \ "title").as[String]) must contain allElementsOf expTitles

        forAll(jsArr) { js =>
          (js \ "type").as[String] mustBe ArchiveFolder.FolderType
          (js \ "path").as[String] must startWith(folder.path)
          (js \ "owner" \ "ownerId").as[String] mustBe s"${Museums.Test.id.underlying}"
          (js \ "createdStamp" \ "by").as[String] mustBe FakeUsers.testWriteId
          (js \ "collection").as[String] mustBe MuseumCollections.Archeology.uuid.asString
        }
      }

      "prevent unauthorized access when listing children for a folder" taggedAs PG in {
        val fid = getArchiveParts(defaultMuseumId).headOption.value.fid
        httpGetTest(
          nhmReadToken,
          folderChildrenUrl(defaultMuseumId, fid)
        ).status mustBe FORBIDDEN
      }

      "return the entire sub-folder tree for an ArchiveFolderItem" taggedAs PG in {
        val folder = getArchiveParts(defaultMuseumId).headOption.value

        val expPaths = Seq(
          folder.path,
          s"${folder.path}/Kong Haakons korrespondanse",
          s"${folder.path}/Kong Haakons korrespondanse/29 september 1789",
          s"${folder.path}/Reisebrev fra historiske personer"
        )

        val res = httpGetTest(
          tokenRead,
          folderTreeUrl(defaultMuseumId, folder.fid)
        )

        res.status mustBe OK
        res.contentType mustBe JSON

        val jsArr = res.json.as[JsArray].value
        jsArr.size mustBe 4
        jsArr.map(js => (js \ "path").as[String]) must contain inOrderElementsOf expPaths
        jsArr.headOption
          .map(js => (js \ "type").as[String])
          .value mustBe ArchivePart.FolderType

        forAll(jsArr.tail) { js =>
          (js \ "type").as[String] mustBe ArchiveFolder.FolderType
        }
      }

      "prevent unauthorized listing of sub-tree for an ArchiveFolderItem" taggedAs PG in {
        val fid = getArchiveParts(defaultMuseumId).headOption.value.fid

        httpGetTest(
          noAccessToken,
          folderTreeUrl(defaultMuseumId, fid)
        ).status mustBe FORBIDDEN
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
