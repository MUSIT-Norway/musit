import no.uio.musit.MusitResults._
import no.uio.musit.models.{
  CollectionUUID,
  MuseumCollection,
  MuseumId,
  StorageNodeDatabaseId
}
import no.uio.musit.security.{AccessAll, AuthenticatedUser, ModuleConstraint}
import no.uio.musit.service.MusitRequest
import play.api.libs.json._
import play.api.mvc.{Result, Results}

import scala.concurrent.{ExecutionContext, Future}

package object controllers {

  val internalErr = (err: MusitError) =>
    Results.InternalServerError(Json.obj("message" -> err.message))

  val badRequestJsErr = (jsErr: JsError) => Results.BadRequest(JsError.toJson(jsErr))

  val badRequestErr = (err: MusitValidationError) =>
    Results.BadRequest(Json.obj("message" -> err.message))

  val musitNotAuthenticatedErr = (err: MusitNotAuthenticated) =>
    Results.Unauthorized(Json.obj("message" -> err.message))

  val musitNotAuthorized = (err: MusitNotAuthorized) =>
    Results.Forbidden(Json.obj("message" -> err.message))

  val badRequestStr = (msg: String) => Results.BadRequest(Json.obj("message" -> msg))

  def invalidUuidResponse(arg: String): Future[Result] = Future.successful {
    Results.BadRequest(Json.obj("message" -> s"Invalid UUID $arg"))
  }

  /**
   * Takes a collection of A's and writes them to a Result with JSON body.
   *
   * @param types the collection of data
   * @param w     an implicit {{{play.api.libs.json.Writes}}} for converting A's to JSON
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
          case MusitSuccess(id)          => Results.Created(Json.toJson(id))
          case err: MusitValidationError => badRequestErr(err)
          case err: MusitError           => internalErr(err)
        }

      case err: JsError =>
        Future.successful(Results.BadRequest(JsError.toJson(err)))
    }
  }

  private def handleUpdateRequest[A, B](
      jsr: JsResult[A]
  )(
      update: A => Future[Result]
  )(
      implicit req: MusitRequest[JsValue],
      ec: ExecutionContext
  ): Future[Result] = {
    jsr match {
      case JsSuccess(at, _) =>
        update(at)

      case err: JsError =>
        Future.successful(Results.BadRequest(JsError.toJson(err)))
    }
  }

  /**
   * Function for saving a data type A as a B, then returning B as the result.
   */
  private[controllers] def updateRequest[A, B](
      jsr: JsResult[A]
  )(
      update: A => Future[MusitResult[B]]
  )(
      implicit req: MusitRequest[JsValue],
      writes: Writes[B],
      ec: ExecutionContext
  ): Future[Result] = {
    handleUpdateRequest(jsr) { a =>
      update(a).map {
        case MusitSuccess(b)              => Results.Ok(Json.toJson(b))
        case valErr: MusitValidationError => badRequestErr(valErr)
        case merr: MusitError             => internalErr(merr)
      }
    }
  }

  private[controllers] def updateRequestOpt[A, B](
      jsr: JsResult[A]
  )(
      update: A => Future[MusitResult[Option[B]]]
  )(
      implicit req: MusitRequest[JsValue],
      writes: Writes[B],
      ec: ExecutionContext
  ): Future[Result] = {
    handleUpdateRequest(jsr) { a =>
      update(a).map {
        case MusitSuccess(b) =>
          b.map(r => Results.Ok(Json.toJson(r))).getOrElse(Results.NoContent)

        case valErr: MusitValidationError => badRequestErr(valErr)
        case merr: MusitError =>
          internalErr(merr)
      }
    }
  }

  type SimpleNode = (StorageNodeDatabaseId, String)

  /**
   * TODO: This is quite dirty... should be improved when time allows.
   */
  def parseCollectionIdsParam(
      mid: MuseumId,
      mod: ModuleConstraint,
      cidsStr: String
  )(implicit currUsr: AuthenticatedUser): Either[Result, Seq[MuseumCollection]] = {
    val colIds = cidsStr
      .split(",")
      .map { ss =>
        val s = ss.trim
        (s, CollectionUUID.fromString(s))
      }
      .toSeq
    val badCols = colIds.filter(_._2.isEmpty)

    if (cidsStr.isEmpty) {
      Left(badRequestStr(s"collectionIds param must contain at least 1 collection ID."))
    } else if (badCols.nonEmpty) {
      Left(badRequestStr(s"Invalid CollectionUUIDs: ${badCols.map(_._1).mkString(", ")}"))
    } else {
      val cids = colIds.filter(_._2.nonEmpty).flatMap(_._2).filter { cid =>
        mod match {
          case AccessAll => currUsr.canAccess(mid, Some(cid))
          case _         => currUsr.canAccess(mid, mod, Some(cid))
        }
      }

      val cols = currUsr.collectionsFor(mid).filter(mc => cids.contains(mc.uuid))

      if (cols.size < colIds.size && !currUsr.hasGodMode) {
        Left(
          Results.Forbidden(
            Json.obj(
              "message" -> (s"Not authorized to access one or more of " +
                s" the following collection(s) $cidsStr")
            )
          )
        )
      } else {
        Right(cols)
      }
    }
  }

}
