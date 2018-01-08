package controllers.conservation

import com.google.inject.{Inject, Singleton}
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.mvc.ControllerComponents
import services.conservation.StorageAndHandlingService
@Singleton
class StorageAndHandlingController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val service: StorageAndHandlingService
) extends MusitController {

  val logger = Logger(classOf[StorageAndHandlingController])
}
