package models

import play.api.libs.json.Json

/**
 * Created by jarle on 06.10.16.
 */
case class MusitThing(museumNo: String, subNo: String, term: String)

object MusitThing {
  implicit val format = Json.format[MusitThing]
}

