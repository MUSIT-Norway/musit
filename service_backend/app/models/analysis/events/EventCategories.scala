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

  val richWrites: Writes[Category] = Writes { cat =>
    Json.obj(
      "id"   -> JsNumber(cat.id),
      "name" -> JsString(cat.entryName)
    )
  }
}

/**
 * Enumeration of different event categories.
 */
object EventCategories extends Enum[Category] {

  val values = findValues

  def fromId(id: Int): Option[Category] = values.find(_.id == id)

  def unsafeFromId(id: Int): Category = fromId(id).get

  case object Chemical extends Category(1)

  case object Colour extends Category(2)

  case object Dating extends Category(3)

  case object Genetic extends Category(4)

  case object Image extends Category(5)

  case object MacroFossil extends Category(6)

  case object MicroFossil extends Category(7)

  case object Morphologic extends Category(8)

  case object Osteological extends Category(9)

  case object Sediment extends Category(10)

  case object SpeciesIdentification extends Category(11)

  case object TextileFiber extends Category(12)

  case object WoodAnatomy extends Category(13)

}
