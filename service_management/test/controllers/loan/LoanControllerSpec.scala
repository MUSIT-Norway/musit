package controllers.loan

import no.uio.musit.models.{MuseumId, ObjectUUID}
import no.uio.musit.security.BearerToken
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import no.uio.musit.time._
import play.api.libs.json.{JsString, Json}
import play.mvc.Http.Status
import play.mvc.Http.Status.CREATED

class LoanControllerSpec extends MusitSpecWithServerPerSuite {
  val mid   = MuseumId(99)
  val token = BearerToken(FakeUsers.testAdminToken)

  val baseUrl = (mid: Int) => s"/$mid/loans"

  val createLoanUrl = baseUrl
  val activeLoanUrl = (mid: Int) => s"/$mid/loans/active"

  "Using the loan controller" when {
    "working with loan" should {
      "create a new loan" in {
        val js = Json.obj(
          "externalRef" -> Json.arr("ref-1", "ref-2"),
          "note"        -> "a note",
          "returnDate"  -> JsString(dateTimeNow.plusMonths(1).toString("yyyy-MM-dd")),
          "objects"     -> Json.arr(ObjectUUID.generate())
        )
        val res =
          wsUrl(createLoanUrl(mid)).withHeaders(token.asHeader).post(js).futureValue

        res.status mustBe CREATED
      }

      "not create new loans without any objects" in {
        val js = Json.obj(
          "externalRef" -> Json.arr("ref-1", "ref-2"),
          "note"        -> "a note",
          "returnDate"  -> JsString(dateTimeNow.plusMonths(1).toString("yyyy-MM-dd")),
          "objects"     -> Json.arr()
        )
        val res =
          wsUrl(createLoanUrl(mid)).withHeaders(token.asHeader).post(js).futureValue

        res.status mustBe Status.BAD_REQUEST
      }
    }
  }
}
