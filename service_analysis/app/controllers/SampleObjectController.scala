package controllers

import com.google.inject.{Inject, Singleton}
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import play.api.Logger
import services.SampleObjectService

@Singleton
class SampleObjectController @Inject() (
    val authService: Authenticator,
    val sampleService: SampleObjectService
) extends MusitController {

  val logger = Logger(classOf[SampleObjectController])


}
