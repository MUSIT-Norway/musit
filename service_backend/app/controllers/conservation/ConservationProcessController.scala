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
) extends MusitController {

  val logger = Logger(classOf[ConservationController])
}
