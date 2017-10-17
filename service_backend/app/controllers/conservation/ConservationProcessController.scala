package controllers.conservation

import com.google.inject.{Inject, Singleton}
import controllers.{internalErr, saveRequest, updateRequestOpt}
import models.conservation.events.{
  ConservationEvent,
  ConservationModuleEvent,
  ConservationProcess
}
import no.uio.musit.MusitResults.{MusitError, MusitSuccess, MusitValidationError}
import no.uio.musit.models.{EventId, EventTypeId, MuseumId}
import no.uio.musit.security.Permissions.{Read, Write}
import no.uio.musit.security.{Authenticator, CollectionManagement}
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.ControllerComponents
import services.conservation.ConservationProcessService

import scala.concurrent.Future

@Singleton
class ConservationProcessController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val consService: ConservationProcessService
) extends MusitController
    with ConservationProcessControllerHelper {

  val logger = Logger(classOf[ConservationController])

  def eventService: ConservationProcessService = consService

  override val eventTypeId = EventTypeId(ConservationProcess.eventTypeId)

  /* Gammelt, kan fjernes. Dette omfavnes nå av ConservationModuleEventKontroller. Den spesifikke
    ConservationProcess-logikken vi behøver ligger i servicen

  def addConservationProcess(mid: MuseumId) =
    MusitSecureAction(mid, CollectionManagement, Write).async(parse.json) {
      implicit request =>
        implicit val currUser = request.user
        val jsr               = request.body.validate[ConservationModuleEvent]
        saveRequest[ConservationModuleEvent, Option[ConservationModuleEvent]](jsr) {
          case proc: ConservationProcess =>
            consService.add(mid, proc)

          case wrong =>
            Future.successful(MusitValidationError("Expected ConservationProcess"))
        }
    }

  def getConservationProcessById(mid: MuseumId, id: EventId) =
    MusitSecureAction(mid, CollectionManagement, Read).async { implicit request =>
      implicit val currUser = request.user
      consService.findConservationProcessById(mid, id).map {
        case MusitSuccess(ma) => ma.map(ae => Ok(Json.toJson(ae))).getOrElse(NotFound)
        case err: MusitError  => internalErr(err)
      }
    }

  def updateConservationProcess(mid: MuseumId, eventId: EventId) =
    MusitSecureAction(mid, CollectionManagement, Write).async(parse.json) {
      implicit request =>
        implicit val currUser = implicitly(request.user)
        val jsr               = request.body.validate[ConservationModuleEvent]
        updateRequestOpt[ConservationModuleEvent, ConservationProcess](jsr) { cp =>
          consService.update(mid, eventId, cp)
        }
    }
 */
}
