package controllers

import no.uio.musit.models.{EventId, MuseumCollections, MuseumId}
import no.uio.musit.security.BearerToken
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite, PostgresContainer => PG}
import org.scalatest.Inspectors.forAll
import org.scalatest.time.{Millis, Span}
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import utils.testdata.{ArchiveableGenerators, BaseDummyData}

import scala.util.Properties.envOrNone

class ModuleAttachmentsControllerIntegrationSpec
    extends MusitSpecWithServerPerSuite
    with ArchiveSpec
    with BaseDummyData
    with ArchiveableGenerators {

  override val timeout =
    envOrNone("MUSIT_FUTURE_TIMEOUT").map(_.toDouble).getOrElse(8 * 1000d)
  override val interval =
    envOrNone("MUSIT_FUTURE_INTERVAL").map(_.toDouble).getOrElse(15d)

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(timeout, Millis),
    interval = Span(interval, Millis)
  )

  val baseUrl                = (mid: Int) => s"/museum/$mid"
  val analysesAttachmentsUrl = (mid: Int) => s"${baseUrl(mid)}/analyses/attachments"
  val downloadAnalysisAttachmentUrl = (mid: Int, fid: String) =>
    s"${analysesAttachmentsUrl(mid)}/$fid"

  def validateJsonRes(
      mid: MuseumId,
      filename: String,
      version: Int,
      path: String,
      createdBy: String,
      js: JsValue
  ): Unit = {
    (js \ "id").asOpt[String] must not be empty
    (js \ "fid").asOpt[String] must not be empty
    (js \ "title").as[String] mustBe filename
    (js \ "fileType").as[String] mustBe "application/pdf"
    (js \ "owner" \ "ownerId").as[String] mustBe s"${mid.underlying}"
    (js \ "owner" \ "ownerType").as[String] mustBe "org"
    (js \ "path").as[String] mustBe path
    (js \ "version").as[Int] mustBe version
    (js \ "published").as[Boolean] mustBe false
    (js \ "createdStamp" \ "by").as[String] mustBe createdBy
    (js \ "createdStamp" \ "date").asOpt[String] must not be empty
    (js \ "documentDetails" \ "number").as[Int] mustBe 1

    addFile(mid, js)
  }

  def testUploadFile(
      filename: String,
      analysisId: EventId,
      tok: BearerToken
  ): WSResponse = {
    val fp = createFilePart(filename, fileSource)

    wsUrl(analysesAttachmentsUrl(defaultMuseumId))
      .withHttpHeaders(tok.asHeader)
      .withQueryStringParameters("analysisId" -> s"${analysisId.underlying}")
      .post(fp)
      .futureValue
  }

  "Using the ModuleAttachmentsController" when {

    "working with analysis results" should {

      "upload a file" taggedAs PG in {
        val res = testUploadFile("testfile1.pdf", 1L, tokenAdmin)

        res.status mustBe CREATED
        res.contentType mustBe JSON

        validateJsonRes(
          defaultMuseumId,
          "testfile1.pdf",
          1,
          "/root/Modules/Analysis/1",
          FakeUsers.testAdminId,
          res.json
        )
      }

      "upload a second file" taggedAs PG in {
        val res = testUploadFile("testfile2.pdf", 1L, tokenAdmin)

        res.status mustBe CREATED
        res.contentType mustBe JSON

        validateJsonRes(
          defaultMuseumId,
          "testfile2.pdf",
          1,
          "/root/Modules/Analysis/1",
          FakeUsers.testAdminId,
          res.json
        )
      }

      "upload a third file" taggedAs PG in {
        val res = testUploadFile("testfile3.pdf", 1L, tokenAdmin)

        res.status mustBe CREATED
        res.contentType mustBe JSON

        validateJsonRes(
          defaultMuseumId,
          "testfile3.pdf",
          1,
          "/root/Modules/Analysis/1",
          FakeUsers.testAdminId,
          res.json
        )
      }

      "upload a file with an existing name for a different analysis" taggedAs PG in {
        val res = testUploadFile("testfile1.pdf", 4L, tokenAdmin)

        res.status mustBe CREATED
        res.contentType mustBe JSON

        validateJsonRes(
          defaultMuseumId,
          "testfile1.pdf",
          1,
          "/root/Modules/Analysis/4",
          FakeUsers.testAdminId,
          res.json
        )
      }

      "prevent file upload without Write access to Collection Management" taggedAs PG in {
        testUploadFile("testfile_fail.pdf", 5L, tokenRead).status mustBe FORBIDDEN
      }

      "prevent uploading when file already exists for given analysis" taggedAs PG in {
        val res = testUploadFile("testfile1.pdf", 4L, tokenAdmin)

        res.status mustBe BAD_REQUEST
        res.contentType mustBe JSON
      }

      "fetch metadata for a list of file ids" taggedAs PG in {
        val res = wsUrl(analysesAttachmentsUrl(defaultMuseumId))
          .withHttpHeaders(tokenAdmin.asHeader)
          .withQueryStringParameters(
            "fileIds" -> addedFiles.result().map(_.fid).mkString(",")
          )
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
            jsAndIndex._1._2._1,
            1,
            s"/root/Modules/Analysis/${jsAndIndex._1._2._2}",
            FakeUsers.testAdminId,
            jsAndIndex._1._1
          )
        }
      }

      "prevent listing files without Read access to Collection Mngmt" taggedAs PG in {
        wsUrl(analysesAttachmentsUrl(defaultMuseumId))
          .withHttpHeaders(noAccessToken.asHeader)
          .withQueryStringParameters("fileIds" -> addedFiles.result().mkString(","))
          .get()
          .futureValue
          .status mustBe FORBIDDEN
      }

      "download a file" taggedAs PG in {
        val fid = addedFiles.result()(1).fid
        val res = wsUrl(downloadAnalysisAttachmentUrl(defaultMuseumId, fid))
          .withHttpHeaders(tokenAdmin.asHeader)
          .get()
          .futureValue

        res.status mustBe OK
        res.contentType mustBe BINARY
      }

      "prevent file download without Read access to Collection Mngmt" taggedAs PG in {
        val fid = addedFiles.result()(1).fid
        wsUrl(downloadAnalysisAttachmentUrl(defaultMuseumId, fid))
          .withHttpHeaders(noAccessToken.asHeader)
          .get()
          .futureValue
          .status mustBe FORBIDDEN
      }

    }

  }
}
