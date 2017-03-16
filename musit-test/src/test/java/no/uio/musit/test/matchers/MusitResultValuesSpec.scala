package no.uio.musit.test.matchers

import no.uio.musit.MusitResults.{MusitEmpty, MusitResult, MusitSuccess}
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

    "return success when type of type MusitError" in {
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
  }
}
