package no.uio.musit.microservices.common.utils

import no.uio.musit.microservices.common.domain.{ MusitError, MusitStatusMessage }
//import play.api.http.Status
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

  def postRoot[A](servicePostCall: A => Future[Either[MusitError, A]], objectToPost: A, toJsonTransformer: A => JsValue) = {
    val res = servicePostCall(objectToPost)
    res.map { either =>
      either.fold(
        l => BadRequest(Json.toJson(l)),
        r => Created(toJsonTransformer(r))
      )
    }
  }

  def updateRoot[A](serviceUpdateCall: (Long, A) => Future[Either[MusitError, MusitStatusMessage]], id: Long, objectToUpdate: A): Future[Result] = {
    serviceUpdateCall(id, objectToUpdate).map {
      case Right(updateStatus) => Ok(Json.toJson(updateStatus))
      case Left(error) => Status(error.status)(Json.toJson(error))
    }
  }

  def updateRootWithJsResult[A](serviceUpdateCall: (Long, A) => Future[Either[MusitError, MusitStatusMessage]], id: Long, validatedResult: JsResult[A]): Future[Result] = {
    validatedResult match {
      case s: JsSuccess[A] => updateRoot(serviceUpdateCall, id, s.get)
      case e: JsError => Future.successful(BadRequest(Json.toJson(MusitError(play.api.http.Status.BAD_REQUEST, e.toString))))
    }
  }

  def getRootFromEither[A](serviceUpdateCall: (Long) => Future[Either[MusitError, A]], id: Long, toJsonTransformer: A => JsValue): Future[Result] = {
    val futResObject = serviceUpdateCall(id)
    futResObject.map {
      case Right(obj) => Ok(toJsonTransformer(obj))
      case Left(error) => Status(error.status)(Json.toJson(error))
    }
  }

}
