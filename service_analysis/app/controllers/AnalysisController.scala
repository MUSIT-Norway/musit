package controllers

import com.google.inject.{Inject, Singleton}
import models.events.AnalysisResults.AnalysisResult
import models.events.{Analysis, AnalysisCollection, EventCategories}
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.models.{CollectionUUID, EventId, ObjectUUID}
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import play.api.libs.json.{JsError, JsSuccess, Json, Writes}
import services.AnalysisService

import scala.concurrent.Future

@Singleton
class AnalysisController @Inject()(
  val authService: Authenticator,
  val analysisService: AnalysisService
) extends MusitController {

  lazy val internalErr = (msg: String) => InternalServerError(Json.obj("message" -> msg))

  private def listAsResult[A](types: Seq[A])(implicit w: Writes[A]) = {
    if (types.nonEmpty) Ok(Json.toJson(types))
    else NoContent
  }

  def getAllAnalysisTypes = MusitSecureAction().async { implicit request =>
    analysisService.getAllTypes.map {
      case MusitSuccess(types) => listAsResult(types)
      case err: MusitError => internalErr(err.message)
    }
  }

  def getAnalysisTypesForCategory(categoryId: Int) =
    MusitSecureAction().async { implicit request =>
      EventCategories.fromId(categoryId).map { c =>
        analysisService.getTypesFor(c).map {
          case MusitSuccess(types) => listAsResult(types)
          case err: MusitError => internalErr(err.message)
        }
      }.getOrElse {
        Future.successful {
          BadRequest(Json.obj("message" -> s"invalid analysis category $categoryId"))
        }
      }
    }

  def getAnalysisTypesForCollection(colUuidStr: String) =
    MusitSecureAction().async { implicit request =>
      CollectionUUID.fromString(colUuidStr).map { cuuid =>
        analysisService.getTypesFor(cuuid).map {
          case MusitSuccess(types) => listAsResult(types)
          case err: MusitError => internalErr(err.message)
        }
      }.getOrElse {
        Future.successful {
          BadRequest(Json.obj("message" -> s"invalid collection UUID $colUuidStr"))
        }
      }
    }

  def getAnalysisById(id: Long) = MusitSecureAction().async { implicit request =>
    val eventId = EventId.fromLong(id)
    analysisService.findById(eventId).map {
      case MusitSuccess(analysis) => Ok(Json.toJson(analysis))
      case err: MusitError => internalErr(err.message)
    }
  }

  def getChildAnalyses(id: Long) = MusitSecureAction().async { implicit request =>
    val eventId = EventId.fromLong(id)
    analysisService.childrenFor(eventId).map {
      case MusitSuccess(analyses) => listAsResult(analyses)
      case err: MusitError => internalErr(err.message)
    }
  }

  def getAnalysisForObject(oid: String) = MusitSecureAction().async { implicit request =>
    ObjectUUID.fromString(oid).map { uuid =>
      analysisService.findByObject(uuid).map {
        case MusitSuccess(analyses) => listAsResult(analyses)
        case err: MusitError => internalErr(err.message)
      }
    }.getOrElse {
      Future.successful(BadRequest(Json.obj("message" -> s"invalid object UUID $oid")))
    }
  }

  def saveAnalysis() = MusitSecureAction().async(parse.json) { implicit request =>
    request.body.validate[Analysis] match {
      case JsSuccess(analysis, _) =>
        analysisService.add(analysis).map {
          case MusitSuccess(eid) => Created
          case err: MusitError => internalErr(err.message)
        }

      case err: JsError =>
        Future.successful(BadRequest(JsError.toJson(err)))
    }
  }

  def saveAnalysisCollection() =
    MusitSecureAction().async(parse.json) { implicit request =>
      request.body.validate[AnalysisCollection] match {
        case JsSuccess(acol, _) =>
          analysisService.add(acol).map {
            case MusitSuccess(eid) => Created
            case err: MusitError => internalErr(err.message)
          }

        case err: JsError =>
          Future.successful(BadRequest(JsError.toJson(err)))
      }
    }

  def saveResult(id: Long) = MusitSecureAction().async(parse.json) { implicit request =>
    val eventId = EventId.fromLong(id)
    request.body.validate[AnalysisResult] match {
      case JsSuccess(result, _) =>
        analysisService.addResult(eventId, result).map {
          case MusitSuccess(eid) => Created
          case err: MusitError => internalErr(err.message)
        }

      case err: JsError =>
        Future.successful(BadRequest(JsError.toJson(err)))
    }
  }
}
