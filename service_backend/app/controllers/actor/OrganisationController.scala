package controllers.actor

import com.google.inject.Inject
import models.actor.Organisation
import no.uio.musit.security.Authenticator
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.service.{MusitController, MusitSearch}
import play.api.libs.json._
import services.actor.OrganisationService
import controllers.{internalErr, listAsPlayResult}
import play.api.mvc.ControllerComponents

import scala.concurrent.Future

class OrganisationController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val orgService: OrganisationService
) extends MusitController {

  def search(
      museumId: Int,
      search: Option[MusitSearch]
  ) = MusitSecureAction().async { request =>
    search match {
      case Some(criteria) =>
        orgService.find(criteria).map(orgs => Ok(Json.toJson(orgs)))

      case None =>
        Future.successful(
          BadRequest(Json.obj("message" -> "Search criteria is required"))
        )
    }
  }

  def get(id: Long) = MusitSecureAction().async { request =>
    orgService.find(id).map {
      case Some(org) =>
        Ok(Json.toJson(org))

      case None =>
        NotFound(Json.obj("message" -> s"Did not find organisation with id: $id"))
    }
  }

  def add = MusitSecureAction().async(parse.json) { request =>
    request.body.validate[Organisation] match {
      case s: JsSuccess[Organisation] =>
        orgService.create(s.get).map(org => Created(Json.toJson(org)))

      case e: JsError =>
        Future.successful(BadRequest(JsError.toJson(e)))
    }
  }

  def update(id: Long) = MusitSecureAction().async(parse.json) { request =>
    request.body.validate[Organisation] match {
      case s: JsSuccess[Organisation] =>
        orgService.update(s.get).map {
          case MusitSuccess(upd) =>
            upd match {
              case Some(updated) =>
                Ok(Json.obj("message" -> s"$updated records were updated!"))
              case None =>
                Ok(Json.obj("message" -> "No records were updated!"))
            }

          case err: MusitError =>
            InternalServerError(Json.obj("message" -> err.message))
        }

      case e: JsError =>
        Future.successful(BadRequest(JsError.toJson(e)))
    }
  }

  def delete(id: Long) = MusitSecureAction().async { request =>
    orgService.remove(id).map { noDeleted =>
      Ok(Json.obj("message" -> s"Deleted $noDeleted record(s)."))
    }
  }

  def getAnalysisLabList =
    MusitSecureAction().async { implicit request =>
      orgService.getAnalysisLabs.map {
        case MusitSuccess(t) => listAsPlayResult(t)
        case err: MusitError => internalErr(err)
      }
    }

}
