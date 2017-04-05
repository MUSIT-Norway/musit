package controllers

import models.analysis.events.SaveCommands.SaveAnalysisEventCommand
import no.uio.musit.MusitResults.{MusitError, MusitResult, MusitSuccess}
import no.uio.musit.service.MusitRequest
import play.api.libs.json._
import play.api.mvc.{Result, Results}

import scala.concurrent.{ExecutionContext, Future}

package object analysis {

  val internalErrMsg = (msg: String) =>
    Results.InternalServerError(Json.obj("message" -> msg))

  val internalErr = (msg: MusitError) => internalErrMsg(msg.message)

  /**
   * Takes a collection of A's and writes them to a Result with JSON body.
   *
   * @param types the collection of data
   * @param w an implicit {{{play.api.libs.json.Writes}}} for converting A's to JSON
   * @tparam A the type of data to transform
   * @return a {{{play.api.mvc.Results}}}.
   */
  private[analysis] def listAsPlayResult[A](types: Seq[A])(
      implicit w: Writes[A]
  ) = {
    if (types.nonEmpty) Results.Ok(Json.toJson(types))
    else Results.NoContent
  }

  /**
   * Convenience function for saving a data type of type A using the given save
   * function. Returns a {{{play.api.mvc.Results.Created}}} if the save function
   * completes successfully.
   */
  private[analysis] def saveRequest[A, ID](
      jsr: JsResult[A]
  )(
      save: A => Future[MusitResult[ID]]
  )(
      implicit req: MusitRequest[JsValue],
      writes: Writes[ID],
      ec: ExecutionContext
  ): Future[Result] = {
    jsr match {
      case JsSuccess(at, _) =>
        save(at).map {
          case MusitSuccess(id) => Results.Created(Json.toJson(id))
          case err: MusitError  => internalErr(err)
        }

      case err: JsError =>
        Future.successful(Results.BadRequest(JsError.toJson(err)))
    }
  }

  /**
   * Function for saving a data type A as a B, then returning B as the result.
   */
  private[analysis] def updateRequest[A, B](
      jsr: JsResult[A]
  )(
      update: A => Future[MusitResult[Option[B]]]
  )(
      implicit req: MusitRequest[JsValue],
      writes: Writes[B],
      ec: ExecutionContext
  ): Future[Result] = {
    jsr match {
      case JsSuccess(at, _) =>
        update(at).map {
          case MusitSuccess(b)  => Results.Ok(Json.toJson(b))
          case merr: MusitError => internalErr(merr)
        }

      case err: JsError =>
        Future.successful(Results.BadRequest(JsError.toJson(err)))
    }
  }

}
