package controllers.loan

import no.uio.musit.models.{MuseumId, ObjectUUID}
import no.uio.musit.security.BearerToken
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import no.uio.musit.time._
import org.scalatest.Inspectors
import play.api.libs.json.{JsArray, JsString, JsValue, Json}
import play.mvc.Http.Status
import play.mvc.Http.Status.CREATED

class LoanControllerSpec extends MusitSpecWithServerPerSuite with Inspectors {
  val mid   = MuseumId(99)
  val token = BearerToken(FakeUsers.testAdminToken)

  val baseUrl = (mid: Int) => s"/$mid/loans"

  val createLoanUrl = baseUrl
  val activeLoanUrl = (mid: Int) => s"/$mid/loans/active"

  private val returnDate = dateTimeNow.plusMonths(1).toString("yyyy-MM-dd")
  private val noteData   = "a note"
  val loan = Json.obj(
    "externalRef" -> Json.arr("ref-1", "ref-2"),
    "note"        -> noteData,
    "returnDate"  -> JsString(returnDate),
    "objects"     -> Json.arr(ObjectUUID.generate()),
    "caseNumbers" -> Json.arr("case1", "case3")
  )

  def verifyLoan(jsv: JsValue) = {
    (jsv \ "note").as[String] mustBe noteData
    (jsv \ "loanType").as[Int] mustBe 2
    (jsv \ "returnDate").as[String] must startWith(returnDate)
    (jsv \ "objects").as[JsArray].value.size mustBe 1
    (jsv \ "caseNumbers").as[JsArray].value.size mustBe 2
  }

  "Using the loan controller" when {
    "working with loan" should {
      "create a new loan" in {

        val res =
          wsUrl(createLoanUrl(mid)).withHeaders(token.asHeader).post(loan).futureValue

        res.status mustBe CREATED
      }

      "not create new loans without any objects" in {
        val js = Json.obj(
          "externalRef" -> Json.arr("ref-1", "ref-2"),
          "note"        -> noteData,
          "returnDate"  -> JsString(dateTimeNow.plusMonths(1).toString("yyyy-MM-dd")),
          "objects"     -> Json.arr()
        )
        val res =
          wsUrl(createLoanUrl(mid)).withHeaders(token.asHeader).post(js).futureValue

        res.status mustBe Status.BAD_REQUEST
      }

      "find active loans" in {
        wsUrl(createLoanUrl(mid)).withHeaders(token.asHeader).post(loan).futureValue
        val res = wsUrl(activeLoanUrl(mid)).withHeaders(token.asHeader).get().futureValue

        res.status mustBe Status.OK
        val array = res.json.as[JsArray].value
        array.size must be > 0
        forAll(array) { it =>
          verifyLoan(it)
        }

      }
    }
  }
}
