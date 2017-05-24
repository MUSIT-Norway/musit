package controllers.analysis

import com.google.inject.{Inject, Singleton}
import models.analysis.events.AnalysisResults.AnalysisResult
import models.analysis.events.SaveCommands._
import models.analysis.events._
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.models.{CollectionUUID, EventId, MuseumId, ObjectUUID}
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import services.analysis.AnalysisService
import controllers._
import scala.concurrent.Future

@Singleton
class AnalysisController @Inject()(
    val authService: Authenticator,
    val analysisService: AnalysisService
) extends MusitController {

  val logger = Logger(classOf[AnalysisController])

  def getAllAnalysisTypes(mid: MuseumId) =
    MusitSecureAction().async { implicit request =>
      analysisService.getAllTypes.map {
        case MusitSuccess(types) => listAsPlayResult(types)
        case err: MusitError     => internalErr(err)
      }
    }

  def getAllAnalysisCategories(mid: MuseumId) =
    MusitSecureAction().async { implicit request =>
      Future.successful(
        listAsPlayResult(EnrichedEventCategories.fromCategories(EventCategories.values))
      )
    }

  def getAnalysisTypesForCategory(mid: MuseumId, categoryId: Int) =
    MusitSecureAction().async { implicit request =>
      EventCategories
        .fromId(categoryId)
        .map { c =>
          analysisService.getTypesFor(c).map {
            case MusitSuccess(types) => listAsPlayResult(types)
            case err: MusitError     => internalErr(err)
          }
        }
        .getOrElse {
          Future.successful {
            BadRequest(Json.obj("message" -> s"Invalid analysis category $categoryId"))
          }
        }
    }

  def getAnalysisTypesForCollection(mid: MuseumId, colUuidStr: String) =
    MusitSecureAction().async { implicit request =>
      CollectionUUID
        .fromString(colUuidStr)
        .map { cuuid =>
          analysisService.getTypesFor(cuuid).map {
            case MusitSuccess(types) => listAsPlayResult(types)
            case err: MusitError     => internalErr(err)
          }
        }
        .getOrElse {
          Future.successful {
            BadRequest(Json.obj("message" -> s"Invalid collection UUID $colUuidStr"))
          }
        }
    }

  def getAnalysisById(mid: MuseumId, id: Long) =
    MusitSecureAction().async { implicit request =>
      val eventId = EventId.fromLong(id)
      analysisService.findById(eventId).map {
        case MusitSuccess(ma) => ma.map(ae => Ok(Json.toJson(ae))).getOrElse(NotFound)
        case err: MusitError  => internalErr(err)
      }
    }

  def getChildAnalyses(mid: MuseumId, id: Long) =
    MusitSecureAction().async { implicit request =>
      val eventId = EventId.fromLong(id)
      analysisService.childrenFor(eventId).map {
        case MusitSuccess(analyses) => listAsPlayResult(analyses)
        case err: MusitError        => internalErr(err)
      }
    }

  def getAnalysisForObject(mid: MuseumId, oid: String) =
    MusitSecureAction().async { implicit request =>
      ObjectUUID
        .fromString(oid)
        .map { uuid =>
          analysisService.findByObject(uuid).map {
            case MusitSuccess(analyses) => listAsPlayResult(analyses)
            case err: MusitError        => internalErr(err)
          }
        }
        .getOrElse {
          Future.successful(
            BadRequest(Json.obj("message" -> s"Invalid object UUID $oid"))
          )
        }
    }

  def saveAnalysisEvent(mid: MuseumId) =
    MusitSecureAction().async(parse.json) { implicit request =>
      implicit val currUser = implicitly(request.user)

      val jsr = request.body.validate[SaveAnalysisEventCommand]

      saveRequest[SaveAnalysisEventCommand, EventId](jsr)(sc => analysisService.add(sc))
    }

  def addResult(mid: MuseumId, id: Long) =
    MusitSecureAction().async(parse.json) { implicit request =>
      implicit val currUser = implicitly(request.user)

      val eventId = EventId.fromLong(id)
      val jsr     = request.body.validate[AnalysisResult]

      saveRequest[AnalysisResult, EventId](jsr) { r =>
        analysisService.addResult(eventId, r)
      }
    }

  def updateResult(mid: MuseumId, eid: Long) =
    MusitSecureAction().async(parse.json) { implicit request =>
      implicit val currUser = implicitly(request.user)

      val eventId = EventId.fromLong(eid)
      val jsr     = request.body.validate[AnalysisResult]

      updateRequest[AnalysisResult, EventId](jsr) { r =>
        analysisService.updateResult(eventId, r)
      }
    }

  def updateAnalysisEvent(mid: MuseumId, eid: Long) =
    MusitSecureAction().async(parse.json) { implicit request =>
      implicit val currUser = implicitly(request.user)

      val eventId = EventId.fromLong(eid)
      val jsr     = request.body.validate[SaveAnalysisEventCommand]

      updateRequestOpt[SaveAnalysisEventCommand, AnalysisEvent](jsr) { sc =>
        analysisService.update(mid, eventId, sc)
      }
    }

}
