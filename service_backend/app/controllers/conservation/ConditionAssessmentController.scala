package controllers.conservation

import com.google.inject.{Inject, Singleton}
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.mvc.ControllerComponents
import services.conservation.ConditionAssessmentService
@Singleton
class ConditionAssessmentController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val service: ConditionAssessmentService
) extends MusitController {

  val logger = Logger(classOf[ConservationController])
}
