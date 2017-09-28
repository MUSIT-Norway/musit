package controllers

import akka.NotUsed
import akka.stream.IOResult
import akka.stream.scaladsl.Source
import akka.util.ByteString
import models.document._
import modules.Bootstrapper
import net.scalytica.symbiotic.test.specs.PostgresSpec
import no.uio.musit.security.BearerToken
import no.uio.musit.test.{FakeUsers, MusitSpecWithApp}
import play.api.libs.json._
import play.api.mvc.MultipartFormData.FilePart
import services.DocumentArchiveService

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait ArchiveSpec extends PostgresSpec {
  self: MusitSpecWithApp =>

  val tokenAdmin    = BearerToken(FakeUsers.testAdminToken)
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
  case class AddedFile(mid: Int, fid: String, path: String)

  val addedFiles   = Seq.newBuilder[AddedFile]
  val addedFolders = List.newBuilder[AddedFolder]

  def addFolder(mid: Int, fid: String, path: String, tpe: String): Unit = {
    addedFolders += AddedFolder(mid, fid, path, tpe)
  }

  def addFolder(mid: Int, js: JsValue): Unit = {
    val fid  = (js \ "fid").as[String]
    val tpe  = (js \ "type").as[String]
    val path = (js \ "path").as[String]

    addFolder(mid, fid, path, tpe)
  }

  def addFile(mid: Int, js: JsValue): Unit = {
    val fid  = (js \ "fid").as[String]
    val tpe  = (js \ "type").as[String]
    val path = (js \ "path").as[String]

    addFile(mid, fid, path)
  }

  def addFile(mid: Int, fid: String, path: String): Unit = {
    addedFiles += AddedFile(mid, fid, path)
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

  def getArchiveDocuments(mid: Int): Seq[AddedFile] =
    addedFiles.result().filter(_.mid == mid)

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

}
