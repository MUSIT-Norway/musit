package controllers.actor

import com.google.inject.Inject
import no.uio.musit.models.ActorId
import no.uio.musit.security.Authenticator
import no.uio.musit.service.{MusitController, MusitSearch}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.ControllerComponents
import services.actor.ActorService

import scala.concurrent.Future
import scala.util.{Failure, Success}

class PersonController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val service: ActorService
) extends MusitController {

  val logger = Logger(classOf[PersonController])

  def search(
      museumId: Int,
      search: Option[MusitSearch]
  ) = MusitSecureAction(museumId).async { request =>
    search match {
      case Some(criteria) =>
        service.findByName(museumId, criteria).map(persons => Ok(Json.toJson(persons)))

      case None =>
        Future.successful(
          BadRequest(Json.obj("message" -> "Search criteria is required"))
        )
    }
  }

  def details = MusitSecureAction().async(parse.json) { implicit request =>
    request.body.validate[Set[ActorId]] match {
      case JsSuccess(ids, path) =>
        service.findDetails(ids).map { persons =>
          if (persons.isEmpty) {
            NoContent
          } else {
            Ok(Json.toJson(persons))
          }
        }

      case e: JsError =>
        Future.successful(BadRequest(Json.obj("message" -> e.toString)))
    }
  }

  def get(id: String) = MusitSecureAction().async { implicit request =>
    ActorId.validate(id) match {
      case Success(uuid) =>
        service.findByActorId(uuid).map {
          case Some(actor) =>
            Ok(Json.toJson(actor))

          case None =>
            NotFound(Json.obj("message" -> s"Did not find object with id: $id"))
        }
      case Failure(ex) =>
        logger.debug("Request contained invalid UUID")
        Future.successful {
          BadRequest(Json.obj("message" -> s"Id $id is not a valid UUID"))
        }
    }
  }
}
