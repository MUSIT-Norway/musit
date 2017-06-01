package models

/**
 * Value class to encode Millimeter precision
 *
 * @param underlying Double representing the mm value.
 */
case class Millimeters(underlying: Double) extends AnyVal

object Millimeters {

  implicit def asDouble(mm: Millimeters): Double = mm.underlying

  implicit def fromDouble(d: Double): Millimeters = Millimeters(d)

}
