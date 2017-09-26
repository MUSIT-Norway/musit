package controllers

import models.document.{Archive, ArchiveItem, BaseFolders}
import net.scalytica.symbiotic.api.types.Path
import net.scalytica.symbiotic.api.types.ResourceParties.Org
import no.uio.musit.models.Museums
import no.uio.musit.models.Museums.Museum
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

  "The DocumentArchiveController" when {

    "adding ArchiveFolderItems" should {

      "return the folder tree from the ArchiveRoot for the Test museum" in {
        val res = wsUrl(foldersBaseUrl(defaultMuseumId))
          .withHttpHeaders(token.asHeader)
          .withQueryStringParameters("includeFiles" -> "false")
          .get()
          .futureValue

        res.status mustBe OK
        res.contentType mustBe JSON

        val resArr = res.json.as[JsArray].value

        resArr.size mustBe 7

        forAll(resArr) { js =>
          (js \ "owner" \ "ownerId").as[String] mustBe defaultMuseumId.underlying.toString
          (js \ "owner" \ "ownerType").as[String] mustBe Org.tpe

          val fid = (js \ "fid").as[String]
          val tpe = (js \ "type").as[String]
          addFolder(defaultMuseumId, fid, tpe)
        }

        val paths = resArr.map(js => (js \ "path").as[String]).map(Path.apply)

        val expectedPaths = Seq(
          Path.root,
          BaseFolders.ModulesFolderPath,
          Path.root.append(Museums.Test.shortName)
        ) ++
          BaseFolders.ModuleFolders.map(_.path(defaultMuseumId))

        paths must contain allElementsOf expectedPaths
      }

      "return the folder tree from the ArchiveRoot for NHM" in {
        val res = wsUrl(foldersBaseUrl(Museums.Nhm.id))
          .withHttpHeaders(nhmReadToken.asHeader)
          .withQueryStringParameters("includeFiles" -> "false")
          .get()
          .futureValue

        res.status mustBe OK
        res.contentType mustBe JSON

        val resArr = res.json.as[JsArray].value

        resArr.size mustBe 7

        forAll(resArr) { js =>
          (js \ "owner" \ "ownerId").as[String] mustBe Museums.Nhm.id.underlying.toString
          (js \ "owner" \ "ownerType").as[String] mustBe Org.tpe

          val fid = (js \ "fid").as[String]
          val tpe = (js \ "type").as[String]
          addFolder(Museums.Nhm.id, fid, tpe)
        }

        val paths = resArr.map(js => (js \ "path").as[String]).map(Path.apply)

        val expectedPaths = Seq(
          Path.root,
          BaseFolders.ModulesFolderPath,
          Path.root.append(Museums.Nhm.shortName)
        ) ++
          BaseFolders.ModuleFolders.map(_.path(defaultMuseumId))

        paths must contain allElementsOf expectedPaths
      }

      "prevent adding an Archive without correct authorization" taggedAs PG in {
        val a      = addArchiveJsonStr("fail", Some("failed archive"))
        val rootId = getArchiveRoot(Museums.Nhm.id).fid

        wsUrl(foldersBaseUrl(Museums.Nhm.id))
          .withHttpHeaders(
            CONTENT_TYPE -> JSON,
            tokenWrite.asHeader // unauthorized token
          )
          .withQueryStringParameters("destFolderId" -> rootId)
          .post(a)
          .futureValue
          .status mustBe FORBIDDEN
      }

      "add an Archive to a museum" taggedAs PG in {
        val a      = addArchiveJsonStr("foobar", Some("fizz buzz description"))
        val rootId = getArchiveRoot(defaultMuseumId).fid

        val res = wsUrl(foldersBaseUrl(defaultMuseumId))
          .withHttpHeaders(
            CONTENT_TYPE -> JSON,
            tokenWrite.asHeader
          )
          .withQueryStringParameters("destFolderId" -> rootId)
          .post(a)
          .futureValue

        res.status mustBe CREATED
        res.contentType mustBe JSON

        (res.json \ "id").asOpt[String] must not be empty
        (res.json \ "fid").asOpt[String] must not be empty
        (res.json \ "title").as[String] mustBe "foobar"
        (res.json \ "description").as[String] mustBe "fizz buzz description"
        (res.json \ "owner" \ "ownerId").as[String] mustBe "99"
        (res.json \ "owner" \ "ownerType").as[String] mustBe "org"
        (res.json \ "path").as[String] mustBe "/root/foobar"
        (res.json \ "published").as[Boolean] mustBe false
        (res.json \ "documentMedium").as[String] mustBe "digital"
        (res.json \ "createdStamp" \ "date").asOpt[String] must not be empty
        (res.json \ "createdStamp" \ "by").asOpt[String] must not be empty
        (res.json \ "type").as[String] mustBe Archive.FolderType
      }

      "add an ArchivePart to the root" taggedAs PG in {
        pending
      }

      "add another ArchivePart to the root" taggedAs PG in {
        pending
      }

      "prevent adding an ArchivePart without correct authorization" taggedAs PG in {
        pending
      }

      "prevent adding an ArchivePart to the root" taggedAs PG in {
        pending
      }

      "add an ArchiveFolder to an ArchivePart" taggedAs PG in {
        pending
      }

      "add another ArchiveFolder to an ArchivePart" taggedAs PG in {
        pending
      }

      "prevent adding an ArchiveFolder without correct authorization" taggedAs PG in {
        pending
      }

      "prevent adding an ArchiveFolder to the root" taggedAs PG in {
        pending
      }

      "add an ArchiveFolder to another ArchiveFolder" taggedAs PG in {
        pending
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
