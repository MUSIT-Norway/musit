package models.storage

import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, _}

case class FromToDouble(from: Option[Double], to: Option[Double])

object FromToDouble {
  implicit val formats: Format[FromToDouble] = (
    (__ \ "from").formatNullable[Double] and
      (__ \ "to").formatNullable[Double]
  )(FromToDouble.apply, unlift(FromToDouble.unapply))

}
