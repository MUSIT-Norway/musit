package no.uio.musit.microservice.event.service

import java.sql.{ Date, Timestamp }

import no.uio.musit.microservice.event.domain.Event

/**
 * Created by jarle on 23.08.16.
 */

object EventOrderingUtils {

  implicit def optionOrdering[A](implicit ao: Ordering[A]): Ordering[Option[A]] =
    new Ordering[Option[A]] {
      override def compare(optX: Option[A], optY: Option[A]): Int = {
        optX match {
          case Some(x) =>
            optY match {
              case Some(y) =>
                ao.compare(x, y)
              case None => 1
            }
          case None =>
            optY match {
              case Some(_) => -1
              case None => 0
            }
        }
      }
    }

  /*
  These two gave infinite recursion, I don't know Scala well enough in order to understand why...

  implicit val dateOrdering: Ordering[Date] = Ordering.comparatorToOrdering[Date]
  implicit val timestampOrdering: Ordering[Timestamp] = Ordering.comparatorToOrdering[Timestamp]
*/

  implicit val utilDateOrdering = new Ordering[java.util.Date] {
    def compare(x: java.util.Date, y: java.util.Date): Int = x compareTo y
  }

  implicit val sqlDateOrdering = new Ordering[java.sql.Date] {
    def compare(x: java.sql.Date, y: java.sql.Date): Int = x compareTo y
  }

  implicit val timestampOrdering = new Ordering[Timestamp] {
    def compare(x: Timestamp, y: Timestamp): Int = x compareTo y
  }

  object EventByDoneDateOrdering extends Ordering[Event] {
    val optDateOrd: Ordering[Option[Date]] = optionOrdering(sqlDateOrdering)
    def compare(eventX: Event, eventY: Event): Int = {
      optDateOrd.compare(eventX.eventDate, eventY.eventDate)
    }
  }

  object EventByRegisteredDateOrdering extends Ordering[Event] {
    val optOrd: Ordering[Option[Timestamp]] = optionOrdering(timestampOrdering)
    def compare(eventX: Event, eventY: Event): Int = {
      optOrd.compare(eventX.registeredDate, eventY.registeredDate)
    }
  }
}
