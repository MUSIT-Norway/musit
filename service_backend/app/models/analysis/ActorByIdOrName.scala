package models.analysis

import no.uio.musit.models.ActorId
import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait ActorByIdOrName {
  def name: String
}

object ActorByIdOrName {

  def apply(str: String): ActorByIdOrName =
    ActorId.fromString(str).map(ActorById.apply).getOrElse(ActorByName(str))

  implicit val reads: Reads[ActorByIdOrName] = Reads { jsv =>
    (jsv \ "type").validate[String].flatMap {
      case ActorByName.key =>
        (jsv \ "value").validate[String].map(ActorByName.apply)
      case ActorById.key =>
        (jsv \ "value").validate[String] match {
          case JsSuccess(str, path) =>
            ActorId
              .fromString(str)
              .map(id => JsSuccess(ActorById(id)))
              .getOrElse(JsError(path, ValidationError(s"Invalid actor id $str")))
          case err: JsError => err
        }
    }
  }

  implicit val writes: Writes[ActorByIdOrName] = Writes {
    case ActorByName(name) =>
      Json.obj(
        "type"  -> ActorByName.key,
        "value" -> name
      )
    case ActorById(id) =>
      Json.obj(
        "type"  -> ActorById.key,
        "value" -> id
      )
  }

}

case class ActorById(actorId: ActorId) extends ActorByIdOrName {
  def name = actorId.underlying.toString
}

object ActorById {
  val key = "ActorById"
}

case class ActorByName(name: String) extends ActorByIdOrName

object ActorByName {
  val key = "ActorByName"
}
