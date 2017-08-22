package services.elasticsearch.shared

import com.sksamuel.elastic4s.analyzers.KeywordAnalyzer
import com.sksamuel.elastic4s.http.ElasticDsl.{
  dateField,
  doubleField,
  objectField,
  textField
}

object FieldConfig {

  def actorSearchStamp(name: String) =
    objectField(name) fields (
      uuid("id"),
      dateField("date"),
      textField("name")
    )

  def actorStamp(name: String) =
    objectField(name) fields (
      uuid("id"),
      textField("name")
    )

  def size = {
    objectField("size") fields (
      textField("unit"),
      doubleField("value")
    )
  }

  def uuid(name: String) =
    textField("uuid") analyzer KeywordAnalyzer
}
