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
