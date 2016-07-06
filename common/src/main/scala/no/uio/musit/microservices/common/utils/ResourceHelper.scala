package no.uio.musit.microservices.common.utils

import no.uio.musit.microservices.common.domain.{ MusitError, MusitStatusMessage }
import no.uio.musit.microservices.common.extensions.FutureExtensions.{ MusitFuture, MusitResult }
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import no.uio.musit.microservices.common.extensions.EitherExtensions._
import no.uio.musit.microservices.common.extensions.FutureExtensions._

/**
 * Misc utilities for creating resources.
 * Created by jstabel on 5/31/16.
 *
 * @author jstabel <jarle.stabell@usit.uio.no>
 *
 */

object ResourceHelper {

  implicit class EitherExtensionsImp[T](val either: Either[MusitError, T]) extends AnyVal {
    /* Not used. (yet?)
    def mapToMusitFuture[S](ifRight: T => Future[Either[MusitError, S]]): Future[Either[MusitError, S]] = {
      either match {
        case Left(l) => Future.successful(Left(l))
        case Right(r) => ifRight(r)
      }
    }
    */

    /**
     * If the either is Right, it is mapped into a future[Result] the obvious way.
     * If it is Left, it is mapped into a successful future containing the error!
     * So beware of further mapping of the result! (This explains the 'Final' in the name.)
     * (Ideally I'd want to make this into a value (of a type) which cannot further be mapped etc on,
     * to prevent potential bugs/misuse.)
     *
     * Even better would be to remove this function! :)
     */
    def mapToFinalPlayResult[S](ifRight: T => Future[Result]): Future[Result] = {
      either match {
        case Left(l) => Future.successful(l.toPlayResult) //This is rather unsemantic,
        // we let go of the MusitError in a "either-failed" MusitFuture and embeds it in a regular future.
        // So from here on, this Future is rather "unsemantic/alien", only to be handled over to the Play Framework.
        case Right(r) => ifRight(r)
      }
    }
  }

  def postRoot[A](objectToPost: MusitFuture[A], toJsonTransformer: A => JsValue): Future[Result] = {
    objectToPost.map {
      _.fold(
        l => l.toPlayResult,
        r => Created(toJsonTransformer(r))
      )
    }
  }

  def postRoot[A](servicePostCall: A => MusitFuture[A], objectToPost: A, toJsonTransformer: A => JsValue): Future[Result] = {
    postRoot(servicePostCall(objectToPost), toJsonTransformer)
  }

  /** Same as postRoot, but the object to post is a MusitResult (Either[MusitError, A]) */
  def postRootWithMusitResult[A](servicePostCall: A => MusitFuture[A], musitResultToPost: Either[MusitError, A],
    toJsonTransformer: A => JsValue): Future[Result] = {
    val musitFutureResult = MusitFuture.fromMusitResult(musitResultToPost) //Was not able to import the .toMusitFuture extension method for some unknown reason... :(
    val result = musitFutureResult.musitFutureFlatMap(servicePostCall)
    postRoot(result, toJsonTransformer)
  }
  /*#OLD
  def postRootWithMusitResult[A](servicePostCall: A => MusitFuture[A], musitResultToPost: Either[MusitError, A],
    toJsonTransformer: A => JsValue) = {
    musitResultToPost.mapToFinalPlayResult {
      objectToPost => postRoot(servicePostCall, objectToPost, toJsonTransformer)
    }
  }
   */

  def updateRoot[A](serviceUpdateCall: (Long, A) => Future[Either[MusitError, MusitStatusMessage]], id: Long, objectToUpdate: A): Future[Result] = {
    serviceUpdateCall(id, objectToUpdate).map {
      case Right(updateStatus) => Ok(Json.toJson(updateStatus))
      case Left(error) => error.toPlayResult
    }
  }

  /** Same as updateRoot, but the object to update is a MusitResult (Either[MusitError, A]) */
  def updateRootWithMusitResult[A](
    serviceUpdateCall: (Long, A) => Future[Either[MusitError, MusitStatusMessage]],
    id: Long, musitResultObjectToUpdate: Either[MusitError, A]
  ): Future[Result] = {
    musitResultObjectToUpdate.mapToFinalPlayResult {
      objectToUpdate => updateRoot(serviceUpdateCall, id, objectToUpdate)
    }
  }

  /* This one is perhaps not used anymore? If so, consider deleting it some time in the future.*/
  def updateRootWithJsResult[A](
    serviceUpdateCall: (Long, A) => Future[Either[MusitError, MusitStatusMessage]],
    id: Long, validatedResult: JsResult[A]
  ): Future[Result] = {
    updateRootWithMusitResult(serviceUpdateCall, id, jsResultToMusitResult(validatedResult))
  }

  def getRoot[A](serviceUpdateCall: (Long) => Future[Either[MusitError, A]], id: Long, toJsonTransformer: A => JsValue): Future[Result] = {
    val futResObject = serviceUpdateCall(id)
    futResObject.map { resObject =>
      resObject match {
        case Right(obj) => Ok(toJsonTransformer(obj))
        case Left(error) => error.toPlayResult
      }
    }
  }

  //Int is expected to be number of records deleted by serviceDeleteCall
  def deleteRoot(serviceDeleteCall: (Long) => Future[Either[MusitError, Int]], id: Long): Future[Result] = {
    serviceDeleteCall(id).map {
      case Right(deleteCount) => {
        if (deleteCount == 0) {
          ErrorHelper.notFound(s"Unable to find object with id: $id to delete").toPlayResult
        } else
          Ok(Json.toJson(MusitStatusMessage(s"Deleted $deleteCount record(s).")))
      }
      case Left(error) => error.toPlayResult
    }
  }

  /*#OLD
  def jsResultToEither[T](jsRes: JsResult[T]): Either[Result, T] = {
    jsRes match {
      case s: JsSuccess[T] => Right(s.value)
      case e: JsError => Left(BadRequest(Json.toJson(e.toString)))
    }
  }
    // TODO: Remove either the below or the above
*/

  def jsResultToMusitResult[T](jsRes: JsResult[T]): Either[MusitError, T] = {
    jsRes match {
      case s: JsSuccess[T] => Right(s.value)
      case e: JsError => Left(ErrorHelper.badRequest(e.toString)) //todo: better way to get the string?
    }
  }

  /** Quite ugly, please don't use this if not absolutely necessary! */
  def musitResultToJsResult[T](musitResult: MusitResult[T]): JsResult[T] = {
    musitResult match {
      case Left(musitError) => JsError(musitError.message)
      case Right(t) => JsSuccess(t)
    }
  }

}

