package services.elasticsearch.client.models

case class Aliases(index: String, aliases: Seq[String])
