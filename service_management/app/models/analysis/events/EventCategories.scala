package models.analysis.events

import enumeratum._
import play.api.libs.json._

/**
 * All analysis events belong to some category. The Category type is the
 * root type for further specifying these Categories. It consists of a single
 * ID value to be able to disambiguate when (de)serializing to/from JSON.
 *
 * @param id An integer identifier used for disambiguation.
 */
sealed abstract class Category(val id: Int) extends EnumEntry

object Category {

  implicit val reads: Reads[Category] = Reads { jsv =>
    jsv.validate[Int] match {
      case JsSuccess(value, path) =>
        EventCategories
          .fromId(value)
          .map(c => JsSuccess(c, path))
          .getOrElse(JsError(path, ""))

      case err: JsError => err
    }
  }

  implicit val writes: Writes[Category] = Writes(cat => JsNumber(cat.id))

}

/**
 * Enumeration of different event categories.
 */
object EventCategories extends Enum[Category] {

  val values = findValues

  def fromId(id: Int): Option[Category] = values.find(_.id == id)

  def unsafeFromId(id: Int): Category = fromId(id).get

  case object AgeIndividual extends Category(1)

  case object Chemical extends Category(2)

  case object ChromosomeCount extends Category(3)

  case object Colour extends Category(4)

  case object Dating extends Category(5)

  case object Genetic extends Category(6)

  case object GeoPhysical extends Category(7)

  case object Image extends Category(8)

  case object Isotope extends Category(9)

  case object LossOnIgnition extends Category(10)

  case object MacroFossil extends Category(11)

  case object MicroFossil extends Category(12)

  case object Morphologic extends Category(13)

  case object Osteological extends Category(14)

  case object Protein extends Category(15)

  case object Sediment extends Category(16)

  case object SpeciesIdentification extends Category(17)

  case object TextileFiber extends Category(18)

  case object UseWear extends Category(19)

  case object Video extends Category(20)

  case object WoodAnatomy extends Category(21)

}
