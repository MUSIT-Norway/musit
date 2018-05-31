package no.uio.musit.models

import play.api.libs.json._

case class PagedResult[A](totalMatches: Int, matches: Seq[A])

object PagedResult {
  implicit def PageResultWrites[T](implicit fmt: Writes[T]): Writes[PagedResult[T]] =
    new Writes[PagedResult[T]] {
      def writes(pr: PagedResult[T]) = Json.obj(
        "totalMatches" -> pr.totalMatches,
        "matches"      -> Json.toJson[Seq[T]](pr.matches)
      )
    }
}
