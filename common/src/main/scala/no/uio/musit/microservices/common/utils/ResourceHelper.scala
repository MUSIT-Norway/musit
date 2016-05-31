package no.uio.musit.microservices.common.utils

import no.uio.musit.microservices.common.domain.{ MusitError, MusitStatusMessage }
import play.api.mvc.Result
import play.api.libs.json._
import play.libs.Json._
import play.api.mvc.Results._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Misc utilities for creating resources.
 * Created by jstabel on 5/31/16.
 *
 * @author jstabel <jarle.stabell@usit.uio.no>
 *
 */

object ResourceHelper {

  def updateRoot[A, L, R](serviceUpdateCall: (Long, A) => Future[Either[MusitError, MusitStatusMessage]], id: Long, validatedResult: JsResult[A], objectTransformer: A => A = identity _): Future[Result] = {
    //val validatedResult: JsResult[A] = request.body.validate[A]
    validatedResult match {
      case s: JsSuccess[A] =>
        val objectToUpdate = objectTransformer(s.get)
        serviceUpdateCall(id, objectToUpdate).map {
          case Right(updateStatus) => Ok(Json.toJson(updateStatus))
          case Left(error) => Status(error.status)(Json.toJson(error))
        }

      case e: JsError => Future.successful(BadRequest(Json.toJson(MusitError(play.api.http.Status.BAD_REQUEST, e.toString))))
    }
  }

}
