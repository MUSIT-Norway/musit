package controllers

import com.google.inject.Inject
import models.MusitThing
import no.uio.musit.service.MusitResults.{MusitDbError, MusitError, MusitSuccess}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller, Results}
import services.ObjectSearchService

class ObjectSearchController @Inject() (
    service: ObjectSearchService
) extends Controller {

  val logger = Logger(classOf[ObjectSearchController])

  private val maxLimit = 100
  private val minLimit = 1
  private val defaultLimit = 25

  def search(mid: Int,
             museumNo: String = "",
             subNo: String = "",
             term: String = "",
             page: Int,
             limit: Int) =
    Action.async { request =>

      val limitToUse = limit match {
        case lim: Int if lim > maxLimit => maxLimit
        case lim: Int if lim < 0 => defaultLimit
        case lim: Int => lim
      }

      service.search(mid, museumNo, subNo, term, page, limitToUse).map {
        case MusitSuccess(res) =>
          Ok(Json.toJson[Seq[MusitThing]](res))

        case MusitDbError(msg, ex) =>
          logger.error(msg, ex.orNull)
          InternalServerError(Json.obj("message" -> msg))

        case err: MusitError =>
          logger.error(err.message)
          Results.InternalServerError(Json.obj("message" -> err.message))
      }
    }
}
