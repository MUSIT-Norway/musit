package controllers.geolocation

import com.google.inject.Inject
import no.uio.musit.MusitResults.{MusitError, MusitHttpError, MusitSuccess}
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.json._
import services.geolocation.GeoLocationService
import play.api.mvc.ControllerComponents

import scala.util.control.NonFatal

class GeoLocationController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val geoLocService: GeoLocationService
) extends MusitController {

  val logger = Logger(classOf[GeoLocationController])

  /**
   * Service for looking up addresses in the geo-location service provided by
   * kartverket.no
   */
  def searchExternal(
      search: Option[String]
  ) = MusitSecureAction().async { implicit request =>
    val expression = search.getOrElse("")
    geoLocService
      .searchGeoNorway(expression)
      .map {
        case MusitSuccess(locations) =>
          Ok(Json.toJson(locations))

        case MusitHttpError(status, msg) =>
          Status(status)

        case err: MusitError =>
          logger.error("InternalServerError: " + s"${err.message}")
          InternalServerError(Json.obj("message" -> err.message))
      }
      .recover {
        case NonFatal(ex) =>
          val msg = "An error occurred when searching for address"
          logger.error(msg, ex)
          InternalServerError(Json.obj("message" -> msg))
      }
  }

}
