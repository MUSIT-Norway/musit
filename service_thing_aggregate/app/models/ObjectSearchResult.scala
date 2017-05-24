package models

import play.api.libs.json.{Json, Writes}

case class ObjectSearchResult(totalMatches: Int, matches: Seq[MusitObject])

object ObjectSearchResult {

  implicit val writes: Writes[ObjectSearchResult] = Json.writes[ObjectSearchResult]

}
