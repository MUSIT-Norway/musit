package no.uio.musit.models

import play.api.libs.json._

object ObjectTypes {

  sealed abstract class ObjectType(val name: String)

  object ObjectType {

    def fromString(str: String): Option[ObjectType] = {
      str match {
        case CollectionObject.name => Some(CollectionObject)
        case SampleObject.name     => Some(SampleObject)
        case Node.name             => Some(Node)
        case _                     => None
      }
    }

    def fromOptString(maybeStr: Option[String]): Option[ObjectType] = {
      maybeStr.flatMap(fromString)
    }

    @throws(classOf[IllegalArgumentException])
    def unsafeFromString(str: String): ObjectType = {
      fromString(str).getOrElse {
        throw new IllegalArgumentException(s"Unsupported ObjectType $str")
      }
    }

    implicit val reads: Reads[ObjectType] = Reads { jsv =>
      val str = jsv.as[String]
      fromString(str)
        .map(ot => JsSuccess(ot))
        .getOrElse(JsError(s"Unknown object type $str"))
    }

    implicit val writes: Writes[ObjectType] = Writes(ot => JsString(ot.name))

  }

  case object CollectionObject extends ObjectType("collection")

  case object SampleObject extends ObjectType("sample")

  case object Node extends ObjectType("node")

}
