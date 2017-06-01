package no.uio.musit.service

object Indices {
  def getFrom(string: String): List[String] =
    string
      .stripPrefix("[")
      .stripSuffix("]")
      .split(",")
      .map(_.trim)
      .filter(_.nonEmpty)
      .sorted
      .toList
}
