import java.net.URLEncoder.encode

import net.scalytica.symbiotic.api.types.FileId
import no.uio.musit.MusitResults._
import no.uio.musit.models.CollectionUUID
import no.uio.musit.models.MuseumCollections.Collection
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound}

package object controllers {

  lazy val ContentDisposition = (filename: String) =>
    s"""attachment; filename="$filename"; filename*=UTF-8''""" +
      encode(filename, "UTF-8").replace("+", "%20")

  def respond[A](res: MusitResult[A])(success: A => Result): Result = {
    res match {
      case MusitSuccess(s)        => success(s)
      case MusitNotFound(msg)     => NotFound(Json.obj("msg" -> msg))
      case MusitGeneralError(msg) => BadRequest(Json.obj("msg" -> msg))
      case err: MusitError        => InternalServerError(Json.obj("msg" -> err.message))
    }
  }

  def parseCollectionIdParam(colId: String): Either[Result, CollectionUUID] = {
    CollectionUUID
      .fromString(colId)
      .map { id =>
        Right(Collection.fromCollectionUUID(id).uuid)
      }
      .getOrElse {
        Left(BadRequest(Json.obj("message" -> s"$colId is not a valid UUID.")))
      }

  }

  def parseMaybeCollectionIdParam(
      maybeColId: Option[String]
  ): Either[Result, Option[CollectionUUID]] = {
    maybeColId.map { colId =>
      parseCollectionIdParam(colId).right.map(Option.apply)
    }.getOrElse {
      Right(None)
    }
  }

  def parseFileIdsParam(fids: String): Either[Result, Seq[FileId]] = {
    val ids = fids.split(",").map(FileId.apply)

    if (ids.nonEmpty) Right(ids)
    else Left(BadRequest(Json.obj("message" -> "fileIds query param was empty")))
  }

}
