package controllers

import com.google.inject.{Inject, Singleton}
import models.events.AnalysisResults.AnalysisResult
import models.events.SaveCommands._
import models.events._
import no.uio.musit.MusitResults.{MusitError, MusitResult, MusitSuccess}
import no.uio.musit.models.{CollectionUUID, EventId, ObjectUUID}
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc.Result
import services.AnalysisService

import scala.concurrent.Future

@Singleton
class AnalysisController @Inject() (
    val authService: Authenticator,
    val analysisService: AnalysisService
) extends MusitController {

  val logger = Logger(classOf[AnalysisController])

  lazy val internalErr = (msg: String) => InternalServerError(Json.obj("message" -> msg))

  private def listAsPlayResult[A](types: Seq[A])(implicit w: Writes[A]) = {
    if (types.nonEmpty) Ok(Json.toJson(types))
    else NoContent
  }

  def getAllAnalysisTypes = MusitSecureAction().async { implicit request =>
    analysisService.getAllTypes.map {
      case MusitSuccess(types) => listAsPlayResult(types)
      case err: MusitError => internalErr(err.message)
    }
  }

  def getAnalysisTypesForCategory(categoryId: Int) =
    MusitSecureAction().async { implicit request =>
      EventCategories.fromId(categoryId).map { c =>
        analysisService.getTypesFor(c).map {
          case MusitSuccess(types) => listAsPlayResult(types)
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
          case MusitSuccess(types) => listAsPlayResult(types)
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
      case MusitSuccess(ma) => ma.map(ae => Ok(Json.toJson(ae))).getOrElse(NotFound)
      case err: MusitError => internalErr(err.message)
    }
  }

  def getChildAnalyses(id: Long) = MusitSecureAction().async { implicit request =>
    val eventId = EventId.fromLong(id)
    analysisService.childrenFor(eventId).map {
      case MusitSuccess(analyses) => listAsPlayResult(analyses)
      case err: MusitError => internalErr(err.message)
    }
  }

  def getAnalysisForObject(oid: String) = MusitSecureAction().async { implicit request =>
    ObjectUUID.fromString(oid).map { uuid =>
      analysisService.findByObject(uuid).map {
        case MusitSuccess(analyses) => listAsPlayResult(analyses)
        case err: MusitError => internalErr(err.message)
      }
    }.getOrElse {
      Future.successful(BadRequest(Json.obj("message" -> s"invalid object UUID $oid")))
    }
  }

  private def saveRequest[A, ID](jsr: JsResult[A])(save: A => Future[MusitResult[ID]])(
    implicit
    req: MusitRequest[JsValue]
  ): Future[Result] = {
    jsr match {
      case JsSuccess(at, _) =>
        save(at).map {
          case MusitSuccess(id) => Created
          case err: MusitError => internalErr(err.message)
        }

      case err: JsError =>
        Future.successful(BadRequest(JsError.toJson(err)))
    }
  }

  def saveAnalysisEvent = MusitSecureAction().async(parse.json) { implicit request =>
    implicit val currUser = implicitly(request.user)
    val js = request.body
    val jsr = js.validate[SaveAnalysis].orElse(js.validate[SaveAnalysisCollection])

    saveRequest[SaveAnalysisEventCommand, EventId](jsr) { sc =>
      analysisService.add(sc.asDomain)
    }
  }

  def saveResult(id: Long) = MusitSecureAction().async(parse.json) { implicit request =>
    implicit val currUser = implicitly(request.user)
    val eventId = EventId.fromLong(id)
    val jsr = request.body.validate[AnalysisResult]

    saveRequest[AnalysisResult, Long](jsr)(r => analysisService.addResult(eventId, r))
  }
}
