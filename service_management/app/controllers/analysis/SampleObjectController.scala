package controllers.analysis

import com.google.inject.{Inject, Singleton}
import models.analysis.SaveSampleObject
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.models.{MuseumId, ObjectUUID}
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, JsSuccess, Json}
import services.analysis.SampleObjectService

import scala.concurrent.Future

@Singleton
class SampleObjectController @Inject() (
    val authService: Authenticator,
    val soService: SampleObjectService
) extends MusitController {

  val logger = Logger(classOf[SampleObjectController])

  def getForMuseum(mid: MuseumId) =
    MusitSecureAction().async { implicit request =>
      soService.findForMuseum(mid).map {
        case MusitSuccess(objects) => listAsPlayResult(objects)
        case err: MusitError => internalErr(err)
      }
    }

  def getById(mid: MuseumId, uuid: String) =
    MusitSecureAction().async { implicit request =>
      ObjectUUID.fromString(uuid).map { oid =>
        soService.findById(oid).map {
          case MusitSuccess(maybeObject) =>
            maybeObject.map(so => Ok(Json.toJson(so))).getOrElse(NotFound)

          case err: MusitError => internalErr(err)

        }
      }.getOrElse {
        Future.successful {
          BadRequest(Json.obj("message" -> s"Invalid object UUID $uuid"))
        }
      }
    }

  def getForParentObject(mid: MuseumId, uuid: String) =
    MusitSecureAction().async { implicit request =>
      ObjectUUID.fromString(uuid).map { oid =>
        soService.findForParent(oid).map {
          case MusitSuccess(objects) => listAsPlayResult(objects)
          case err: MusitError => internalErr(err)

        }
      }.getOrElse {
        Future.successful {
          BadRequest(Json.obj("message" -> s"Invalid object UUID $uuid"))
        }
      }
    }

  def save(mid: MuseumId) =
    MusitSecureAction().async(parse.json) { implicit request =>
      implicit val currUser = implicitly(request.user)

      saveRequest(request.body.validate[SaveSampleObject]) { cso =>
        soService.add(cso.asSampleObject)
      }
    }

  def update(mid: MuseumId, uuid: String) =
    MusitSecureAction().async(parse.json) { implicit request =>
      implicit val currUser = implicitly(request.user)
      ObjectUUID.fromString(uuid).map { oid =>
        request.body.validate[SaveSampleObject] match {
          case JsSuccess(saveSampleObject, _) =>
            soService.update(oid, saveSampleObject.asSampleObject).map {
              case MusitSuccess(mso) =>
                mso.map(u => Ok(Json.toJson(u))).getOrElse(NotFound)

              case err: MusitError =>
                internalErr(err)
            }

          case err: JsError =>
            Future.successful(BadRequest(JsError.toJson(err)))
        }
      }.getOrElse {
        Future.successful(
          BadRequest(Json.obj("message" -> s"Invalid object UUID $uuid"))
        )
      }

    }

}
