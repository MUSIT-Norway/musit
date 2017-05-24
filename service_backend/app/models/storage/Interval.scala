package models.storage

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Interval[T](base: T, tolerance: Option[Int])

object Interval {
  /*
     See: https://github.com/playframework/playframework/issues/5261

     The above issue makes it impossible to use the JSON macros for generating
     JSON reads/writes/formats for `Interval`. Because it contains a recursive
     type `Option[T]`.

     Therefore the JSON picklers are hand-written.

   */
  //  implicit val intFmt: Format[Interval[Int]] = Json.format[Interval[Int]]
  //  implicit val longFmt: Format[Interval[Long]] = Json.format[Interval[Long]]
  //  implicit val dblFmt: Format[Interval[Double]] = Json.format[Interval[Double]]

  implicit val intFormat: Format[Interval[Int]] = (
    (__ \ "base").format[Int] and
      (__ \ "tolerance").formatNullable[Int]
  )((b, t) => Interval(b, t), i => (i.base, i.tolerance))

  implicit val longFormat: Format[Interval[Long]] = (
    (__ \ "base").format[Long] and
      (__ \ "tolerance").formatNullable[Int]
  )((b, t) => Interval(b, t), i => (i.base, i.tolerance))

  implicit val doubleFormat: Format[Interval[Double]] = (
    (__ \ "base").format[Double] and
      (__ \ "tolerance").formatNullable[Int]
  )((b, t) => Interval(b, t), i => (i.base, i.tolerance))
}
