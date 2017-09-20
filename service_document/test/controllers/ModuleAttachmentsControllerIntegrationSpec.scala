package controllers

import akka.stream.scaladsl.Source
import modules.Bootstrapper
import net.scalytica.symbiotic.test.specs.PostgresSpec
import no.uio.musit.models.{CollectionUUID, EventId, MuseumCollections, MuseumId}
import no.uio.musit.security.BearerToken
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import org.scalatest.Inspectors.forAll
import org.scalatest.time.{Millis, Span}
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.mvc.MultipartFormData.FilePart
import play.api.test.Helpers._
import services.DocumentArchiveService
import utils.testdata.{ArchiveableGenerators, BaseDummyData}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Properties.envOrNone

class ModuleAttachmentsControllerIntegrationSpec
    extends MusitSpecWithServerPerSuite
    with PostgresSpec
    with BaseDummyData
    with ArchiveableGenerators {

  override val timeout =
    envOrNone("MUSIT_FUTURE_TIMEOUT").map(_.toDouble).getOrElse(5 * 1000d)
  override val interval =
    envOrNone("MUSIT_FUTURE_INTERVAL").map(_.toDouble).getOrElse(15d)

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(timeout, Millis),
    interval = Span(interval, Millis)
  )

  private[this] val service = fromInstanceCache[DocumentArchiveService]

  override def initDatabase(): Either[String, Unit] = {
    val res = super.initDatabase()

    Await.result(Bootstrapper.init(service.dmService), 5 seconds)

    res
  }

  val baseUrl                = (mid: Int) => s"/museum/$mid"
  val analysesAttachmentsUrl = (mid: Int) => s"${baseUrl(mid)}/analyses/attachments"
  val downloadAnalysisAttachmentUrl = (mid: Int, fid: String) =>
    s"${analysesAttachmentsUrl(mid)}/$fid"

  val token         = BearerToken(FakeUsers.testAdminToken)
  val tokenWrite    = BearerToken(FakeUsers.testWriteToken)
  val tokenRead     = BearerToken(FakeUsers.testReadToken)
  val noAccessToken = BearerToken(FakeUsers.testUserToken)

  val addedFiles = Seq.newBuilder[String]

  def testUploadFile(
      filename: String,
      analysisId: EventId,
      collection: MuseumCollections.Collection,
      tok: BearerToken
  ): WSResponse = {
    val fp = Source.single(
      FilePart(
        key = "file",
        filename = filename,
        contentType = Some("application/pdf"),
        ref = fileSource
      )
    )
    wsUrl(analysesAttachmentsUrl(defaultMuseumId))
      .withHttpHeaders(tok.asHeader)
      .withQueryStringParameters(
        "analysisId"   -> s"${analysisId.underlying}",
        "collectionId" -> collection.uuid.asString
      )
      .post(fp)
      .futureValue
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

  "Using the ModuleAttachmentsController" when {

    "working with analysis results" should {

      "upload a file" in {
        val res = testUploadFile("testfile1.pdf", 1L, MuseumCollections.Archeology, token)

        res.status mustBe CREATED
        res.contentType mustBe JSON

        validateJsonRes(
          defaultMuseumId,
          MuseumCollections.Archeology.uuid,
          "testfile1.pdf",
          1,
          "/root/Modules/Analysis/1",
          FakeUsers.testAdminId,
          res.json
        )
      }

      "upload a second file" in {
        val res = testUploadFile("testfile2.pdf", 1L, MuseumCollections.Archeology, token)

        res.status mustBe CREATED
        res.contentType mustBe JSON

        validateJsonRes(
          defaultMuseumId,
          MuseumCollections.Archeology.uuid,
          "testfile2.pdf",
          1,
          "/root/Modules/Analysis/1",
          FakeUsers.testAdminId,
          res.json
        )
      }

      "upload a third file" in {
        val res = testUploadFile("testfile3.pdf", 1L, MuseumCollections.Archeology, token)

        res.status mustBe CREATED
        res.contentType mustBe JSON

        validateJsonRes(
          defaultMuseumId,
          MuseumCollections.Archeology.uuid,
          "testfile3.pdf",
          1,
          "/root/Modules/Analysis/1",
          FakeUsers.testAdminId,
          res.json
        )
      }

      "upload a file with an existing name for a different analysis" in {
        val res = testUploadFile("testfile1.pdf", 4L, MuseumCollections.Archeology, token)

        res.status mustBe CREATED
        res.contentType mustBe JSON

        validateJsonRes(
          defaultMuseumId,
          MuseumCollections.Archeology.uuid,
          "testfile1.pdf",
          1,
          "/root/Modules/Analysis/4",
          FakeUsers.testAdminId,
          res.json
        )
      }

      "not allow uploading a file without Write access to Collection Management" in {
        testUploadFile(
          "testfile_fail.pdf",
          5L,
          MuseumCollections.Archeology,
          tokenRead
        ).status mustBe FORBIDDEN
      }

      "fetch metadata for a list of file ids" in {
        val res = wsUrl(analysesAttachmentsUrl(defaultMuseumId))
          .withHttpHeaders(token.asHeader)
          .withQueryStringParameters("fileIds" -> addedFiles.result().mkString(","))
          .get()
          .futureValue

        res.status mustBe OK
        res.contentType mustBe JSON

        val expectedValues = Seq(
          "testfile1.pdf" -> "1",
          "testfile2.pdf" -> "1",
          "testfile3.pdf" -> "1",
          "testfile1.pdf" -> "4"
        )

        val resArr = res.json.as[JsArray].value

        forAll(resArr.zip(expectedValues).zipWithIndex) { jsAndIndex =>
          validateJsonRes(
            defaultMuseumId,
            MuseumCollections.Archeology.uuid,
            jsAndIndex._1._2._1,
            1,
            s"/root/Modules/Analysis/${jsAndIndex._1._2._2}",
            FakeUsers.testAdminId,
            jsAndIndex._1._1
          )
        }
        // TODO: Parse JSON array
      }

      "not allow fetching file ids without Read access to Collection Management" in {
        wsUrl(analysesAttachmentsUrl(defaultMuseumId))
          .withHttpHeaders(noAccessToken.asHeader)
          .withQueryStringParameters("fileIds" -> addedFiles.result().mkString(","))
          .get()
          .futureValue
          .status mustBe FORBIDDEN
      }

      "download a file" in {
        val fid = addedFiles.result()(1)
        val res = wsUrl(downloadAnalysisAttachmentUrl(defaultMuseumId, fid))
          .withHttpHeaders(token.asHeader)
          .get()
          .futureValue

        res.status mustBe OK
        res.contentType mustBe BINARY
      }

      "not allow downloading a file without Read access to Collection Management" in {
        val fid = addedFiles.result()(1)
        wsUrl(downloadAnalysisAttachmentUrl(defaultMuseumId, fid))
          .withHttpHeaders(noAccessToken.asHeader)
          .get()
          .futureValue
          .status mustBe FORBIDDEN
      }

    }

  }
}
