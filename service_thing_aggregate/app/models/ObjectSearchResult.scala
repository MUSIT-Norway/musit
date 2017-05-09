package models

import play.api.libs.json.{Format, Json}

case class ObjectSearchResult(totalMatches: Int, matches: Seq[MusitObject])

object ObjectSearchResult {

  implicit val format: Format[ObjectSearchResult] = Json.format[ObjectSearchResult]

}
