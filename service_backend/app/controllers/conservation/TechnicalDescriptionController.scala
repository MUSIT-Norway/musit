package controllers.conservation

import com.google.inject.{Inject, Singleton}
import models.conservation.events.{ConservationEvent, TechnicalDescription}
import no.uio.musit.models.EventTypeId
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.mvc.ControllerComponents
import services.conservation.{ConservationEventService, TechnicalDescriptionService}
@Singleton
class TechnicalDescriptionController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val service: TechnicalDescriptionService
) extends MusitController
    with ConservationEventControllerHelper {

  val logger = Logger(classOf[ConservationController])

  val eventTypeId = EventTypeId(TechnicalDescription.eventTypeId)

  def eventService = service.asInstanceOf[ConservationEventService[ConservationEvent]]
}
/*  Gammelt, kan fjernes. Dette omfavnes nå av ConservationModuleEventKontroller. Den spesifikke
    logikken vi behøver ligger i servicen/nevnte controller og kan redefineres ved en subklasse av denne

  def addTechnicalDescription(mid: MuseumId) =
    MusitSecureAction(mid, CollectionManagement, Write).async(parse.json) {
      implicit request =>
        implicit val currUser = request.user
        val jsr               = request.body.validate[ConservationModuleEvent]
        saveRequest[ConservationModuleEvent, Option[ConservationModuleEvent]](jsr) {
          case proc: TechnicalDescription =>
            service.add(mid, proc)

          case wrong =>
            Future.successful(MusitValidationError("Expected TechnicalDescription"))
        }

    }

  def getTechnicalDescriptionById(mid: MuseumId, id: EventId) =
    MusitSecureAction(mid, CollectionManagement, Read).async { implicit request =>
      implicit val currUser = request.user
      service.findConservationEventById(mid, id).map {
        case MusitSuccess(ma) => ma.map(ae => Ok(Json.toJson(ae))).getOrElse(NotFound)
        case err: MusitError  => internalErr(err)
      }
    }

  def updateTechnicalDescription(mid: MuseumId, eventId: EventId) = ???
//    MusitSecureAction(mid, CollectionManagement, Write).async(parse.json) {
//      implicit request =>
//        implicit val currUser = implicitly(request.user)
//        val jsr               = request.body.validate[ConservationModuleEvent]
//        updateRequestOpt[ConservationModuleEvent, ConservationProcess](jsr) { cp =>
//          service.update(mid, eventId, cp)
//        }
//    }

}
 */
