package no.uio.musit.test.matchers

import no.uio.musit.test.matchers.DateTimeEquivalence._
import org.joda.time.DateTime
import org.scalactic.TripleEquals._

trait DateTimeMatchers {

  final class DateTimeMatcher(val leftSideValue: DateTime) {
    def mustApproximate(right: DateTime): Unit = leftSideValue === right
  }

  final class OptDateTimeMatcher(val leftSideValue: Option[DateTime]) {
    def mustApproximate(right: Option[DateTime]): Unit = leftSideValue === right
  }

  implicit def convertToDateTimeMatcher(a: DateTime): DateTimeMatcher = {
    new DateTimeMatcher(a)
  }

  implicit def convertToOptDateTimeMatcher(a: Option[DateTime]): OptDateTimeMatcher = {
    new OptDateTimeMatcher(a)
  }
}

object DateTimeMatchers extends DateTimeMatchers
