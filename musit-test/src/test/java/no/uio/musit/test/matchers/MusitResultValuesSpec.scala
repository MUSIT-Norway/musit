package no.uio.musit.test.matchers

import java.sql.SQLException

import no.uio.musit.MusitResults.{MusitDbError, MusitEmpty, MusitResult, MusitSuccess}
import org.scalatest.{Inside, MustMatchers, WordSpec}

import scala.util.{Failure, Success, Try}

class MusitResultValuesSpec
    extends WordSpec
    with MusitResultValues
    with MustMatchers
    with Inside {

  "MusitResultValues" should {

    "return success when type is MusitSuccess" in {
      val res = Try { MusitSuccess(1).successValue }

      res mustBe a[Success[_]]
    }

    "return failure when type is MusitError" in {
      val res = Try { MusitEmpty.successValue }

      res mustBe a[Failure[_]]
    }

    "failure message must have the expected type" in {
      val musitResult: MusitResult[String] = MusitEmpty

      val res = Try { musitResult.successValue }

      inside(res) {
        case Failure(t) => t.getMessage must include("MusitSuccess[String]")
      }
    }

    "include origin stacktrace from MusitDbError" in {
      val exception = new SQLException("Exception from test")
      val musitResult: MusitResult[String] =
        MusitDbError("db message", Some(exception))

      val res = Try { musitResult.successValue }

      inside(res) {
        case Failure(t) => t.getCause mustBe exception
      }
    }
  }
}
