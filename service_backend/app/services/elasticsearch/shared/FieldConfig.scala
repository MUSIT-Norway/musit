package services.elasticsearch.shared

import com.sksamuel.elastic4s.http.ElasticDsl.{
  dateField,
  doubleField,
  objectField,
  textField
}

object FieldConfig {

  def actorSearchStamp(name: String) =
    objectField(name) fields (
      textField("id"),
      dateField("date"),
      textField("name")
    )

  def actorStamp(name: String) =
    objectField(name) fields (textField("id"),
    textField("name"))

  def size = {
    objectField("size") fields (
      textField("unit"),
      doubleField("value")
    )
  }
}
