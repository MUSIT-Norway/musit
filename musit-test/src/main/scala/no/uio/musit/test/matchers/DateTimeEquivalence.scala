package no.uio.musit.test.matchers

import org.joda.time.DateTime
import org.scalactic._
import TripleEquals._

/**
 * Convenience implementation to check for time equivalence. The assertion will
 * ensure the DateTime instances are within ten seconds of each other.
 * Finer granularity is skipped from comparison due to the very common
 * use-case where one or the other side has less precision available. E.g.
 * Date in JSON bodies coming from web browsers might loose their milliseconds,
 * while the server will not.
 */
object DateTimeEquivalence {

  private val TenSecondsAsMillis = 10000L

  implicit val dateTimeApproxEq = new Equality[DateTime] {
    def areEqual(a: DateTime, b: Any): Boolean =
      b match {
        case p: DateTime =>
          (p.getMillis - a.getMillis) <= TenSecondsAsMillis ||
            (a.getMillis - p.getMillis) <= TenSecondsAsMillis

        case _ => false
      }
  }

  implicit val optDateTimeApproxEq = new Equality[Option[DateTime]] {
    def areEqual(a: Option[DateTime], b: Any): Boolean = {
      b match {
        case Some(dt) =>
          dt match {
            case p: DateTime => a.exists(_ === p)
          }
        case None => a.isEmpty
      }
    }
  }

}
