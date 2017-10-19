package controllers.conservation

import com.google.inject.{Inject, Singleton}
import models.conservation.events.{ConservationEvent, Treatment}
import no.uio.musit.models.EventTypeId
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.mvc.ControllerComponents
import services.conservation.{ConservationEventService, TreatmentService}
@Singleton
class TreatmentController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val service: TreatmentService
) extends MusitController
    with ConservationEventControllerHelper {

  val logger = Logger(classOf[ConservationController])

  val eventTypeId = EventTypeId(Treatment.eventTypeId)

  def eventService = service.asInstanceOf[ConservationEventService[ConservationEvent]]
}
