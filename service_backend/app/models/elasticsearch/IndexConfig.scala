package models.elasticsearch

import com.sksamuel.elastic4s.indexes.CreateIndexDefinition

case class IndexConfig(indexName: String, alias: String, mapping: CreateIndexDefinition)
