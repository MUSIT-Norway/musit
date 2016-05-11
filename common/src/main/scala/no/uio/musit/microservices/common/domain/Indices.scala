package no.uio.musit.microservices.common.domain

/**
  * Created by jarlandre on 08/05/16.
  */
object Indices {
  def getFrom(string: String): List[String] = string
    .stripPrefix("[")
    .stripSuffix("]")
    .split(",")
    .map(_.trim)
    .filter(_.nonEmpty)
    .sorted
    .toList
}
