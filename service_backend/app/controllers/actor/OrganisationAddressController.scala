package controllers.actor

import com.google.inject.Inject
import models.actor.OrganisationAddress
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.ControllerComponents
import services.actor.OrganisationAddressService

import scala.concurrent.Future

class OrganisationAddressController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val orgAdrService: OrganisationAddressService
) extends MusitController {

  val logger = Logger(classOf[OrganisationAddressController])

  def listForOrg(organisationId: Long) =
    MusitSecureAction().async { implicit request =>
      orgAdrService.all(organisationId).map(addr => Ok(Json.toJson(addr)))
    }

  def get(organisationId: Long, id: Long) =
    MusitSecureAction().async { implicit request =>
      orgAdrService.find(organisationId, id).map {
        case Some(addr) =>
          Ok(Json.toJson(addr))

        case None =>
          NotFound(Json.obj("message" -> s"Did not find object with id: $id"))
      }
    }

  def add(organisationId: Long) =
    MusitSecureAction().async(parse.json) { implicit request =>
      request.body.validate[OrganisationAddress] match {
        case s: JsSuccess[OrganisationAddress] =>
          orgAdrService.create(organisationId, s.get).map { newAddr =>
            Created(Json.toJson(newAddr))
          }

        case e: JsError =>
          Future.successful(BadRequest(JsError.toJson(e)))
      }
    }

  def update(organisationId: Long, id: Long) =
    MusitSecureAction().async(parse.json) { implicit request =>
      request.body.validate[OrganisationAddress] match {
        case s: JsSuccess[OrganisationAddress] =>
          orgAdrService.update(s.get).map {
            case MusitSuccess(upd) =>
              upd match {
                case Some(updated) =>
                  Ok(Json.obj("message" -> "Record was updated!"))
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

  def delete(organisationId: Long, id: Long) =
    MusitSecureAction().async { implicit request =>
      orgAdrService.remove(id).map { noDeleted =>
        Ok(Json.obj("message" -> s"Deleted $noDeleted record(s)."))
      }
    }
}
