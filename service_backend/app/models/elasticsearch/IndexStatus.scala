package models.elasticsearch

import org.joda.time.DateTime

case class IndexStatus(
    alias: String,
    indexed: DateTime,
    updated: Option[DateTime]
)
