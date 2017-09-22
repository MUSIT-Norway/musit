package controllers

import no.uio.musit.models.{EventId, MuseumCollections}
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
    envOrNone("MUSIT_FUTURE_TIMEOUT").map(_.toDouble).getOrElse(5 * 1000d)
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

  def testUploadFile(
      filename: String,
      analysisId: EventId,
      collection: MuseumCollections.Collection,
      tok: BearerToken
  ): WSResponse = {
    val fp = createFilePart(filename, fileSource)

    wsUrl(analysesAttachmentsUrl(defaultMuseumId))
      .withHttpHeaders(tok.asHeader)
      .withQueryStringParameters(
        "analysisId"   -> s"${analysisId.underlying}",
        "collectionId" -> collection.uuid.asString
      )
      .post(fp)
      .futureValue
  }

  "Using the ModuleAttachmentsController" when {

    "working with analysis results" should {

      "upload a file" taggedAs PG in {
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

      "upload a second file" taggedAs PG in {
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

      "upload a third file" taggedAs PG in {
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

      "upload a file with an existing name for a different analysis" taggedAs PG in {
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

      "prevent file upload without Write access to Collection Management" taggedAs PG in {
        testUploadFile(
          "testfile_fail.pdf",
          5L,
          MuseumCollections.Archeology,
          tokenRead
        ).status mustBe FORBIDDEN
      }

      "prevent uploading when file already exists for given analysis" taggedAs PG in {
        val res = testUploadFile("testfile1.pdf", 4L, MuseumCollections.Archeology, token)

        res.status mustBe BAD_REQUEST
        res.contentType mustBe JSON
      }

      "fetch metadata for a list of file ids" taggedAs PG in {
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

      "prevent listing files without Read access to Collection Mngmt" taggedAs PG in {
        wsUrl(analysesAttachmentsUrl(defaultMuseumId))
          .withHttpHeaders(noAccessToken.asHeader)
          .withQueryStringParameters("fileIds" -> addedFiles.result().mkString(","))
          .get()
          .futureValue
          .status mustBe FORBIDDEN
      }

      "download a file" taggedAs PG in {
        val fid = addedFiles.result()(1)
        val res = wsUrl(downloadAnalysisAttachmentUrl(defaultMuseumId, fid))
          .withHttpHeaders(token.asHeader)
          .get()
          .futureValue

        res.status mustBe OK
        res.contentType mustBe BINARY
      }

      "prevent file download without Read access to Collection Mngmt" taggedAs PG in {
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
