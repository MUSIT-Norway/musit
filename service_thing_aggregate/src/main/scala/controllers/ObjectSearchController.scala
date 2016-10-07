package controllers

import com.google.inject.Inject
import models.MusitThing
import no.uio.musit.service.MusitResults.{MusitDbError, MusitError, MusitResult, MusitSuccess}
import play.api.Logger
import play.api.mvc.{Action, Controller, Result}
import services.{ObjectAggregationService, ObjectSearchService, StorageNodeService}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import play.api.mvc.Results

import scala.concurrent.Future

/**
 * Created by jarle on 05.10.16.
 */

object Utils {

  def musitResultToPlayResult[A](musitResult: MusitResult[A], toJsonTransformer: A => JsValue): Result = {
    musitResult match {
      case MusitSuccess(res) =>
        Results.Ok(toJsonTransformer(res))

      case MusitDbError(msg, ex) =>
        Logger.error(msg, ex.orNull)
        Results.InternalServerError(msg)

      case r: MusitError =>
        Results.InternalServerError(r.message)
    }
  }
}

class ObjectSearchController @Inject() (
    service: ObjectSearchService
) extends Controller {

  def search(mid: Int, museumNo: String = "", subNo: String = "", term: String = "", page: Int = 1, limit: Int = 25) =
    Action.async { request =>

      val limitToUse = Math.max(limit, service.maxLimit)
      /*      if (limit > service.maxLimit) {

        Future.successful(BadRequest(s"limit (${limit} is too high.")) //TODO: Should we tell the limit?
      } else
*/
      def toJson(ob: Seq[MusitThing]) = Json.toJson(ob)

      service.search(mid, museumNo, subNo, term, page, limitToUse).map(mr => Utils.musitResultToPlayResult(mr, toJson))
    }

}
