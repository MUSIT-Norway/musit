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

  def getAnalysisTypes(
      mid: MuseumId,
      categoryId: Option[Int],
      collectionIds: Option[String]
  ) =
    MusitSecureAction().async { implicit request =>
      implicit val currUser = implicitly(request.user)
      val maybeCat          = categoryId.flatMap(EventCategories.fromId)
      val maybeColl         = collectionIds.flatMap(CollectionUUID.fromString)
      analysisService.getTypesFor(maybeCat, maybeColl).map {
        case MusitSuccess(types) => listAsPlayResult(types)
        case err: MusitError     => internalErr(err)
      }
    }

  def getAllAnalysisCategories(mid: MuseumId) =
    MusitSecureAction().async { implicit request =>
      Future.successful(
        listAsPlayResult(EventCategories.values)(Category.richWrites)
      )
    }

  def getAnalysisById(mid: MuseumId, id: Long) =
    MusitSecureAction().async { implicit request =>
      val eventId = EventId.fromLong(id)
      analysisService.findById(mid, eventId).map {
        case MusitSuccess(ma) => ma.map(ae => Ok(Json.toJson(ae))).getOrElse(NotFound)
        case err: MusitError  => internalErr(err)
      }
    }

  def getChildAnalyses(mid: MuseumId, id: Long) =
    MusitSecureAction().async { implicit request =>
      val eventId = EventId.fromLong(id)
      analysisService.childrenFor(mid, eventId).map {
        case MusitSuccess(analyses) => listAsPlayResult(analyses)
        case err: MusitError        => internalErr(err)
      }
    }

  def getAnalysisForObject(mid: MuseumId, oid: String) =
    MusitSecureAction().async { implicit request =>
      ObjectUUID
        .fromString(oid)
        .map { uuid =>
          analysisService.findByObject(mid, uuid).map {
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

      saveRequest[SaveAnalysisEventCommand, Option[AnalysisModuleEvent]](jsr)(
        sc => analysisService.add(mid, sc)
      )
    }

  def addResult(mid: MuseumId, id: Long) =
    MusitSecureAction().async(parse.json) { implicit request =>
      implicit val currUser = implicitly(request.user)

      val eventId = EventId.fromLong(id)
      val jsr     = request.body.validate[AnalysisResult]

      saveRequest[AnalysisResult, EventId](jsr) { r =>
        analysisService.addResult(mid, eventId, r)
      }
    }

  def updateResult(mid: MuseumId, eid: Long) =
    MusitSecureAction().async(parse.json) { implicit request =>
      implicit val currUser = implicitly(request.user)

      val eventId = EventId.fromLong(eid)
      val jsr     = request.body.validate[AnalysisResult]

      updateRequest[AnalysisResult, EventId](jsr) { r =>
        analysisService.updateResult(mid, eventId, r)
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

  def getAnalysisEvents(mid: MuseumId) =
    MusitSecureAction().async { implicit request =>
      analysisService.findAnalysisEvents(mid).map {
        case MusitSuccess(res) => listAsPlayResult(res)
        case err: MusitError   => InternalServerError(Json.obj("message" -> err.message))
      }
    }
}
