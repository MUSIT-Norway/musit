package models

import no.uio.musit.service.{BindableOf, Indices}

case class OrgSearch (searchMap: Map[String, String], searchStrings: List[String])

object OrgSearch {

  val empty = OrgSearch(Map.empty, List.empty)

  @throws(classOf[IllegalArgumentException])
  def parseParams(p: List[String]): Map[String, String] =
    p.foldLeft(Map[String, String]()) { (acc, next) =>
      next.split("=") match {
        case Array(key, value) if value.nonEmpty =>
          acc + (key -> value)

        case other =>
          throw new IllegalArgumentException(
            s"Syntax error in (part of) search part of URL: $next"
          )
      }
    }

  @throws(classOf[IllegalArgumentException])
  def parseSearch(search: String): OrgSearch =
    "^\\[(.*)\\]$".r.findFirstIn(search) match {
      case Some(string) =>
        val indices = Indices.getFrom(string)
        OrgSearch(
          parseParams(indices.filter(_.contains('='))),
          indices.filterNot(_.contains("="))
        )

      case _ =>
        empty
    }

  implicit val queryBinder =
    new BindableOf[OrgSearch](_.map(v => Right(parseSearch(v.trim))))
}
