package controllers.conservation

import com.google.inject.{Inject, Singleton}
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.mvc.ControllerComponents
import services.conservation.MeasurementDeterminationService
@Singleton
class MeasurementDeterminationController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val service: MeasurementDeterminationService
) extends MusitController {

  val logger = Logger(classOf[MeasurementDeterminationController])
}
