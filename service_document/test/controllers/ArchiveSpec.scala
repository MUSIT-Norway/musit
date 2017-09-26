package controllers

import akka.NotUsed
import akka.stream.IOResult
import akka.stream.scaladsl.Source
import akka.util.ByteString
import models.document._
import modules.Bootstrapper
import net.scalytica.symbiotic.test.specs.PostgresSpec
import no.uio.musit.models.{CollectionUUID, MuseumId}
import no.uio.musit.security.BearerToken
import no.uio.musit.test.{FakeUsers, MusitSpecWithApp}
import play.api.libs.json._
import play.api.mvc.MultipartFormData.FilePart
import services.DocumentArchiveService

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait ArchiveSpec extends PostgresSpec {
  self: MusitSpecWithApp =>

  val token         = BearerToken(FakeUsers.testAdminToken)
  val tokenWrite    = BearerToken(FakeUsers.testWriteToken)
  val tokenRead     = BearerToken(FakeUsers.testReadToken)
  val noAccessToken = BearerToken(FakeUsers.testUserToken)
  val nhmReadToken  = BearerToken(FakeUsers.nhmReadToken)

  private[this] val service = fromInstanceCache[DocumentArchiveService]

  override def initDatabase(): Either[String, Unit] = {
    val res = super.initDatabase()

    Await.result(Bootstrapper.init(service.dmService), 5 seconds)

    res
  }

  case class AddedFolder(mid: Int, fid: String, path: String, tpe: String)

  val addedFiles   = Seq.newBuilder[String]
  val addedFolders = List.newBuilder[AddedFolder]

  def addFolder(mid: Int, fid: String, path: String, tpe: String): Unit = {
    addedFolders += AddedFolder(mid, fid, path, tpe)
  }

  def addFolder(mid: Int, js: JsValue): Unit = {
    val fid  = (js \ "fid").as[String]
    val tpe  = (js \ "type").as[String]
    val path = (js \ "path").as[String]

    addedFolders += AddedFolder(mid, fid, path, tpe)
  }

  def findFolder(mid: Int, fid: String, tpe: String): Option[AddedFolder] =
    addedFolders.result().find(af => af.mid == mid && af.fid == fid && af.tpe == tpe)

  def getArchiveRoot(mid: Int): AddedFolder =
    addedFolders
      .result()
      .filter(f => f.mid == mid && f.tpe == ArchiveRoot.FolderType)
      .head

  def getArchive(mid: Int): AddedFolder =
    addedFolders.result().filter(f => f.mid == mid && f.tpe == Archive.FolderType).head

  def getArchiveParts(mid: Int): Seq[AddedFolder] =
    addedFolders.result().filter(f => f.mid == mid && f.tpe == ArchivePart.FolderType)

  def getArchiveFolders(mid: Int): Seq[AddedFolder] =
    addedFolders.result().filter(f => f.mid == mid && f.tpe == ArchiveFolder.FolderType)

  def getGenericFolders(mid: Int): Seq[AddedFolder] =
    addedFolders.result().filter(f => f.mid == mid && f.tpe == GenericFolder.FolderType)

  def createFilePart(
      filename: String,
      fileSource: Source[ByteString, Future[IOResult]]
  ): Source[FilePart[Source[ByteString, Future[IOResult]]], NotUsed] = {
    Source.single(
      FilePart(
        key = "file",
        filename = filename,
        contentType = Some("application/pdf"),
        ref = fileSource
      )
    )
  }

  def validateJsonRes(
      mid: MuseumId,
      colId: CollectionUUID,
      filename: String,
      version: Int,
      path: String,
      createdBy: String,
      js: JsValue
  ) = {
    (js \ "id").asOpt[String] must not be empty
    (js \ "fid").asOpt[String] must not be empty
    (js \ "title").as[String] mustBe filename
    (js \ "fileType").as[String] mustBe "application/pdf"
    (js \ "owner" \ "ownerId").as[String] mustBe s"${mid.underlying}"
    (js \ "owner" \ "ownerType").as[String] mustBe "org"
    (js \ "collection").as[String] mustBe colId.asString
    (js \ "path").as[String] mustBe path
    (js \ "version").as[Int] mustBe version
    (js \ "published").as[Boolean] mustBe false
    (js \ "createdStamp" \ "by").as[String] mustBe createdBy
    (js \ "createdStamp" \ "date").asOpt[String] must not be empty
    (js \ "documentDetails" \ "number").as[Int] mustBe 1

    addedFiles += (js \ "fid").as[String]
  }

}
