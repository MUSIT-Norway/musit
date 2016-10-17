package no.uio.musit.service

case class MusitSearch(searchMap: Map[String, String], searchStrings: List[String])

object MusitSearch {

  def parseParams(p: List[String]): Map[String, String] =
    p.foldLeft(Map[String, String]())((acc, next) => next.split("=") match {
      case Array(key, value) if value.nonEmpty =>
        acc + (key -> value)
      case other => throw new IllegalArgumentException(s"Syntax error in (part of) search part of URL: $next")
    })

  def parseSearch(search: String): MusitSearch =
    "^\\[(.*)\\]$".r.findFirstIn(search) match {
      case Some(string) =>
        val indices = Indices.getFrom(string)
        MusitSearch(
          parseParams(indices.filter(_.contains('='))),
          indices.filterNot(_.contains("="))
        )
      case _ =>
        MusitSearch(Map(), List())
    }

  implicit val queryBinder = new BindableOf[MusitSearch](_.map(v => Right(parseSearch(v.trim))))
}

