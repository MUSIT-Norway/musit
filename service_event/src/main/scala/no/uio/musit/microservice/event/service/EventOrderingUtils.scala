package no.uio.musit.microservice.event.service

import java.sql.{ Date, Timestamp }

import no.uio.musit.microservice.event.domain.Event

/**
 * Created by jarle on 23.08.16.
 */

object EventOrderingUtils {
  /*If I had known Scala well, I'd be able to get something similar to the following to work...

  implicit def optionOrdering[T <: Ordered[T], A <: Option[T]]: Ordering[A] = new Ordering[A] {
    override def compare(optX: A, optY: A): Int = {
      optX match {
        case Some(x) =>
          optY match {
            case Some(y) => x.compareTo(y)
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
*/

  implicit def dateOrdering[A <: Option[Date]]: Ordering[A] = new Ordering[A] {
    override def compare(optX: A, optY: A): Int = {
      optX match {
        case Some(x) =>
          optY match {
            case Some(y) => x.compareTo(y)
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

  implicit def timestampOrdering[A <: Option[Timestamp]]: Ordering[A] = new Ordering[A] {
    override def compare(optX: A, optY: A): Int = {
      optX match {
        case Some(x) =>
          optY match {
            case Some(y) => x.compareTo(y)
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

  implicit def eventByRegisteredDateOrdering[A <: Event]: Ordering[A] = new Ordering[A] {
    override def compare(eventX: A, eventY: A): Int = {
      timestampOrdering.compare(eventX.registeredDate, eventY.registeredDate)
    }
  }
}
