import no.uio.musit.MusitResults.{
  MusitError,
  MusitResult,
  MusitSuccess,
  MusitValidationError
}
import no.uio.musit.service.MusitRequest
import play.api.libs.json._
import play.api.mvc.{Result, Results}

import scala.concurrent.{ExecutionContext, Future}

package object controllers {

  val internalErr = (err: MusitError) =>
    Results.InternalServerError(Json.obj("message" -> err.message))

  val badRequestErr = (err: MusitValidationError) =>
    Results.BadRequest(Json.obj("message" -> err.message))

  /**
   * Takes a collection of A's and writes them to a Result with JSON body.
   *
   * @param types the collection of data
   * @param w an implicit {{{play.api.libs.json.Writes}}} for converting A's to JSON
   * @tparam A the type of data to transform
   * @return a {{{play.api.mvc.Results}}}.
   */
  private[controllers] def listAsPlayResult[A](types: Seq[A])(
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
  private[controllers] def saveRequest[A, ID](
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
          case err: MusitValidationError =>
            Results.BadRequest(Json.obj("message" -> err.message))
          case err: MusitError => internalErr(err)
        }

      case err: JsError =>
        Future.successful(Results.BadRequest(JsError.toJson(err)))
    }
  }

}
