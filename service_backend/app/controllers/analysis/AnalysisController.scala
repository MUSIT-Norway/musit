package controllers.analysis

import com.google.inject.{Inject, Singleton}
import controllers._
import models.analysis.events.AnalysisResults.AnalysisResult
import models.analysis.events.SaveCommands._
import models.analysis.events._
import no.uio.musit.MusitResults.{MusitError, MusitSuccess, MusitValidationError}
import no.uio.musit.models.{CollectionUUID, EventId, MuseumId, ObjectUUID}
import no.uio.musit.security.Permissions.{Read, Write}
import no.uio.musit.security.{Authenticator, CollectionManagement}
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.ControllerComponents
import services.analysis.AnalysisService

import scala.concurrent.Future

@Singleton
class AnalysisController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val analysisService: AnalysisService
) extends MusitController {

  val logger = Logger(classOf[AnalysisController])

  def getAnalysisTypes(
      mid: MuseumId,
      categoryId: Option[Int],
      collectionIds: Option[String]
  ) =
    MusitSecureAction(mid, CollectionManagement).async { implicit request =>
      implicit val currUser = request.user

      val maybeCat  = categoryId.flatMap(EventCategories.fromId)
      val maybeColl = collectionIds.flatMap(CollectionUUID.fromString)
      analysisService.getTypesFor(maybeCat, maybeColl).map {
        case MusitSuccess(types) => listAsPlayResult(types)
        case err: MusitError     => internalErr(err)
      }
    }

  def getAllAnalysisCategories(mid: MuseumId) =
    MusitSecureAction(mid, CollectionManagement, Read).async { implicit request =>
      Future.successful(
        listAsPlayResult(EventCategories.values)(Category.richWrites)
      )
    }

  def getAnalysisById(mid: MuseumId, id: Long) =
    MusitSecureAction(mid, CollectionManagement, Read).async { implicit request =>
      implicit val currUser = request.user
      analysisService.findById(mid, id).map {
        case MusitSuccess(ma) => ma.map(ae => Ok(Json.toJson(ae))).getOrElse(NotFound)
        case err: MusitError  => internalErr(err)
      }
    }

  def getChildAnalyses(mid: MuseumId, id: Long) =
    MusitSecureAction(mid, CollectionManagement, Read).async { implicit request =>
      implicit val currUser = request.user
      analysisService.childrenFor(mid, id).map {
        case MusitSuccess(analyses) => listAsPlayResult(analyses)
        case err: MusitError        => internalErr(err)
      }
    }

  def getAnalysisForObject(mid: MuseumId, oid: String) =
    MusitSecureAction(mid, CollectionManagement, Read).async { implicit request =>
      implicit val currUser = request.user

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
    MusitSecureAction(mid, CollectionManagement, Write).async(parse.json) {
      implicit request =>
        implicit val currUser = request.user

        val jsr = request.body.validate[SaveAnalysisEventCommand]

        saveRequest[SaveAnalysisEventCommand, Option[AnalysisModuleEvent]](jsr)(
          sc => analysisService.add(mid, sc)
        )
    }

  def addResult(mid: MuseumId, id: Long) =
    MusitSecureAction(mid, CollectionManagement, Write).async(parse.json) {
      implicit request =>
        implicit val currUser = request.user
        val jsr               = request.body.validate[AnalysisResult]

        saveRequest[AnalysisResult, EventId](jsr) { r =>
          analysisService.addResult(mid, id, r)
        }
    }

  def updateResult(mid: MuseumId, eid: Long) =
    MusitSecureAction(mid, CollectionManagement, Write).async(parse.json) {
      implicit request =>
        implicit val currUser = request.user
        val jsr               = request.body.validate[AnalysisResult]

        updateRequest[AnalysisResult, EventId](jsr) { r =>
          analysisService.updateResult(mid, eid, r)
        }
    }

  def importResults(mid: MuseumId, eid: Long) =
    MusitSecureAction(mid, CollectionManagement, Write).async(parse.json) {
      implicit request =>
        implicit val currUser = request.user

        request.body.validate[AnalysisResultImport] match {
          case JsSuccess(resImport, _) =>
            analysisService.updateResults(mid, eid, resImport).map {
              case MusitSuccess(())              => Ok
              case invalid: MusitValidationError => badRequestErr(invalid)
              case err: MusitError               => internalErr(err)
            }

          case err: JsError => Future.successful(badRequestJsErr(err))
        }
    }

  def updateAnalysisEvent(mid: MuseumId, eid: Long) =
    MusitSecureAction(mid, CollectionManagement, Write).async(parse.json) {
      implicit request =>
        implicit val currUser = implicitly(request.user)
        val jsr               = request.body.validate[SaveAnalysisEventCommand]

        updateRequestOpt[SaveAnalysisEventCommand, AnalysisEvent](jsr) { sc =>
          analysisService.update(mid, eid, sc)
        }
    }

  def getAnalysisEvents(mid: MuseumId, collectionIds: String) =
    MusitSecureAction(mid, CollectionManagement, Read).async { implicit request =>
      implicit val currUser = request.user

      parseCollectionIdsParam(mid, collectionIds)(request.user) match {
        case Left(res) =>
          Future.successful(res)

        case Right(cids) =>
          analysisService.findAnalysisEvents(mid, cids).map {
            case MusitSuccess(res) =>
              listAsPlayResult(res)

            case err: MusitError =>
              InternalServerError(Json.obj("message" -> err.message))
          }
      }
    }
}
