package models.analysis

import java.util.UUID

import no.uio.musit.models.ActorId
import play.api.libs.json._

import scala.util.{Failure, Success, Try}

sealed trait ActorName {
  def name: String
}

object ActorName {

  def apply(str: String): ActorName =
    Try(UUID.fromString(str)).toOption
      .map(ActorId.apply)
      .map(ActorById.apply)
      .getOrElse(ActorByName(str))

  implicit val reads: Reads[ActorName] = Reads { jsv =>
    (jsv \ "type").validate[String].flatMap {
      case ActorByName.key =>
        (jsv \ "name").validate[String].map(ActorByName.apply)
      case ActorById.key =>
        (jsv \ "id").validate[String].flatMap { str =>
          Try(UUID.fromString(str)) match {
            case Success(id) => JsSuccess(ActorById(ActorId(id)))
            case Failure(t)  => JsError(s"Invalid actor id $str ")
          }
        }
    }
  }

  implicit val writes: Writes[ActorName] = Writes {
    case ActorByName(name) =>
      Json.obj(
        "type" -> ActorByName.key,
        "name" -> name
      )
    case ActorById(id) =>
      Json.obj(
        "type" -> ActorById.key,
        "id"   -> id
      )
  }

}

case class ActorById(actorId: ActorId) extends ActorName {
  def name = actorId.toString
}

object ActorById {
  val key = "ActorById"
}

case class ActorByName(name: String) extends ActorName

object ActorByName {
  val key = "ActorByName"
}
