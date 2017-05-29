package models.analysis.events

import play.api.libs.json.{Format, Json, Reads}

/**
 * Created by jarl on 24/05/17.
 */
case class EnrichedEventCategories(name: String, id: Int)

object EnrichedEventCategories {
  implicit val reads: Format[EnrichedEventCategories] =
    Json.format[EnrichedEventCategories]

  def fromCategories(categories: Seq[Category]): Seq[EnrichedEventCategories] =
    categories.map(cat => EnrichedEventCategories(cat.entryName, cat.id))
}
