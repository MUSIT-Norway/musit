package utils

import models.analysis.events.AnalysisResults.{AnalysisResult, AgeResult, GenericResult}
import no.uio.musit.models.ActorId
import no.uio.musit.test.matchers.DateTimeMatchers
import org.joda.time.DateTime
import org.scalatest.MustMatchers

trait AnalysisValidators extends DateTimeMatchers with MustMatchers {

  def validateResult[A <: AnalysisResult](
      actual: A,
      expected: A,
      expRegBy: Option[ActorId] = None,
      expRegDate: Option[DateTime] = None
  ) = {
    actual match {
      case gr: GenericResult =>
        validateGenericResult(
          gr,
          expected.asInstanceOf[GenericResult],
          expRegBy,
          expRegDate
        )

      case dr: AgeResult =>
        validateDatingResult(
          dr,
          expected.asInstanceOf[AgeResult],
          expRegBy,
          expRegDate
        )
    }
  }

  def validateGenericResult(
      actual: GenericResult,
      expected: GenericResult,
      expRegBy: Option[ActorId] = None,
      expRegDate: Option[DateTime] = None
  ) = {
    actual.comment mustBe expected.comment
    actual.extRef mustBe expected.extRef
    actual.registeredBy mustBe expRegBy
    actual.registeredDate mustApproximate expRegDate
  }

  def validateDatingResult(
      actual: AgeResult,
      expected: AgeResult,
      expRegBy: Option[ActorId] = None,
      expRegDate: Option[DateTime] = None
  ) = {
    actual.comment mustBe expected.comment
    actual.extRef mustBe expected.extRef
    actual.age mustBe expected.age
    actual.registeredBy mustBe expRegBy
    actual.registeredDate mustApproximate expRegDate
  }

}
