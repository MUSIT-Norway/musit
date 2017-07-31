package services.elasticsearch.client.models

import play.api.libs.json._
import ElasticsearchConfig.empty

/**
 * Filed properties are based on the list provided here:
 * https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-types.html
 *
 * This is just a wrapper over the json structure and it might not include all the options.
 * The intention of having the wrapper is to make it a bit more safe to write mappers
 *
 */
case class ElasticsearchConfig(types: Set[IndexMapping])

object ElasticsearchConfig {
  val empty: JsObject = JsObject(Seq.empty[(String, JsValue)])

  implicit val writes: Writes[ElasticsearchConfig] = Writes[ElasticsearchConfig] { m =>
    Json.obj("mappings" -> m.types.foldLeft(empty) { (json, t) =>
      json ++ Json.toJson[IndexMapping](t).as[JsObject]
    })
  }
}

case class IndexMapping(
    name: String,
    properties: Set[FieldProperties],
    parent: Option[String] = None
)

object IndexMapping {

  implicit val writes: Writes[IndexMapping] = Writes[IndexMapping] { typ =>
    Json.obj(
      typ.name ->
        (Json.obj("properties" -> typ.properties.foldLeft(empty) { (jsObj, p) =>
          jsObj ++ Json.toJson[FieldProperties](p).as[JsObject]
        }) ++ typ.parent
          .map(p => Json.obj("_parent" -> Json.obj("type" -> JsString(p))))
          .getOrElse(empty))
    )
  }
}

sealed trait FieldProperties {
  val name: String
  val typ: Option[String]
  val format: Option[String] = None
}

object FieldProperties {
  implicit val writes: Writes[FieldProperties] = Writes[FieldProperties] {
    case of: ObjectField =>
      Json.obj(
        of.name -> (of.format.map { format =>
          Json.obj("format" -> JsString(format))
        }.getOrElse(empty)
          ++ Json.obj("properties" -> of.properties.foldLeft(empty) { (jsObj, p) =>
            jsObj ++ Json.toJson[FieldProperties](p).as[JsObject]
          }))
      )

    case nf: NestedField =>
      Json.obj(
        nf.name -> Json.obj(
          "type" -> nf.typ.map(JsString.apply).getOrElse[JsValue](JsNull),
          "properties" -> nf.properties.foldLeft(empty) { (jsObj, p) =>
            jsObj ++ Json.toJson[FieldProperties](p).as[JsObject]
          }
        )
      )

    case fp =>
      Json.obj(
        fp.name -> (Json.obj(
          "type" -> fp.typ.map(JsString.apply).getOrElse[JsValue](JsNull)
        ) ++
          fp.format.map { format =>
            Json.obj("format" -> JsString(format))
          }.getOrElse(empty))
      )
  }
}

/**
 * Number value integer
 *
 * Note: this is a naive implementation and we should probably implement all number types,
 * but it will be good enough for now.
 */
case class IntegerField(name: String) extends FieldProperties {
  val typ = Some("integer")
}

/**
 * Number value of double
 */
case class DoubleField(name: String) extends FieldProperties {
  val typ = Some("double")
}

/**
 * Boolean value
 */
case class BooleanField(name: String) extends FieldProperties {
  val typ = Some("boolean")
}

/**
 * String value
 */
case class TextField(name: String) extends FieldProperties {
  val typ = Some("text")
}

/**
 * A keyword of a single string value
 */
case class KeywordField(name: String) extends FieldProperties {
  val typ = Some("keyword")
}

/**
 * Date datatype
 */
case class DateField(name: String, override val format: Option[String] = None)
    extends FieldProperties {
  val typ = Some("date")
}

/**
 * Object for single JSON objects
 */
case class ObjectField(name: String, properties: Set[FieldProperties])
    extends FieldProperties {
  val typ = None
}

/**
 * Nested for arrays of JSON objects
 */
case class NestedField(name: String, properties: Set[FieldProperties])
    extends FieldProperties {
  val typ = Some("nested")

}
