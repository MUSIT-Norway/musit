package controllers.conservation

import com.google.inject.{Inject, Singleton}
import controllers.{internalErr, saveRequest}
import models.conservation.events.{ConservationEvent, Treatment}
import no.uio.musit.MusitResults.{
  MusitError,
  MusitResult,
  MusitSuccess,
  MusitValidationError
}
import no.uio.musit.models.{EventId, MuseumId}
import no.uio.musit.security.Permissions.{Read, Write}
import no.uio.musit.security.{Authenticator, CollectionManagement}
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import services.conservation.TreatmentService

import scala.concurrent.Future
@Singleton
class TreatmentController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val service: TreatmentService
) extends MusitController {

  val logger = Logger(classOf[ConservationController])

  def addTreatment(mid: MuseumId) =
    MusitSecureAction(mid, CollectionManagement, Write).async(parse.json) {
      implicit request =>
        implicit val currUser = request.user
        implicit val _        = Treatment.reads
        val jsr               = request.body.validate[Treatment]
        saveRequest[ConservationEvent, Option[ConservationEvent]](jsr) {
          case proc: Treatment =>
            service.add(mid, proc)

          case wrong =>
            Future.successful(MusitValidationError("Expected Treatment"))
        }

    }

  def getTreatmentById(mid: MuseumId, id: EventId) =
    MusitSecureAction(mid, CollectionManagement, Read).async { implicit request =>
      implicit val currUser = request.user

      def findById(): Future[MusitResult[Option[Treatment]]] =
        service.findConservationEventById(mid, id)

      findById().map {
        case MusitSuccess(ma) => ma.map(ae => Ok(Json.toJson(ae))).getOrElse(NotFound)
        case err: MusitError  => internalErr(err)
      }
    }

  //  def updateConservationProcess(mid: MuseumId, eventId: EventId) =
  //    MusitSecureAction(mid, CollectionManagement, Write).async(parse.json) {
  //      implicit request =>
  //        implicit val currUser = implicitly(request.user)
  //        val jsr               = request.body.validate[ConservationModuleEvent]
  //        updateRequestOpt[ConservationModuleEvent, ConservationProcess](jsr) { cp =>
  //          service.update(mid, eventId, cp)
  //        }
  //    }

}
