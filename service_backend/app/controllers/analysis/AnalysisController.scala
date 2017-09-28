package controllers.analysis

import com.google.inject.{Inject, Singleton}
import controllers._
import models.analysis.events.AnalysisResults.AnalysisResult
import models.analysis.events.SaveCommands._
import models.analysis.events._
import no.uio.musit.MusitResults.{MusitError, MusitSuccess, MusitValidationError}
import no.uio.musit.models.{CollectionUUID, EventId, MuseumId, ObjectUUID}
import no.uio.musit.security.{AuthenticatedUser, Authenticator}
import no.uio.musit.security.Permissions.Read
import no.uio.musit.service.MusitController
import play.api.{Configuration, Logger}
import play.api.libs.json._
import play.api.mvc.ControllerComponents
import services.analysis.AnalysisService
import services.elasticsearch.search.AnalysisSearchService

import scala.concurrent.Future

@Singleton
class AnalysisController @Inject()(
    val conf: Configuration,
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val analysisService: AnalysisService,
    val analysisSearchService: AnalysisSearchService
) extends MusitController {

  val logger = Logger(classOf[AnalysisController])

  val maxLimitConfKey     = "musit.analysis.search.max-limit"
  val defaultLimitConfKey = "musit.analysis.search.default-limit"

  private val maxLimit     = conf.getOptional[Int](maxLimitConfKey).getOrElse(100)
  private val defaultLimit = conf.getOptional[Int](defaultLimitConfKey).getOrElse(25)

  private def calcLimit(l: Int): Int = l match {
    case lim: Int if lim > maxLimit => maxLimit
    case lim: Int if lim < 0        => defaultLimit
    case lim: Int                   => lim
  }

  def getAnalysisTypes(
      mid: MuseumId,
      categoryId: Option[Int],
      collectionIds: Option[String]
  ) =
    MusitSecureAction().async { implicit request =>
      implicit val currUser: AuthenticatedUser = request.user

      val maybeCat  = categoryId.flatMap(EventCategories.fromId)
      val maybeColl = collectionIds.flatMap(CollectionUUID.fromString)
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
      implicit val currUser: AuthenticatedUser = request.user
      analysisService.findById(mid, id).map {
        case MusitSuccess(ma) => ma.map(ae => Ok(Json.toJson(ae))).getOrElse(NotFound)
        case err: MusitError  => internalErr(err)
      }
    }

  def getChildAnalyses(mid: MuseumId, id: Long) =
    MusitSecureAction().async { implicit request =>
      implicit val currUser: AuthenticatedUser = request.user
      analysisService.childrenFor(mid, id).map {
        case MusitSuccess(analyses) => listAsPlayResult(analyses)
        case err: MusitError        => internalErr(err)
      }
    }

  def getAnalysisForObject(mid: MuseumId, oid: String) =
    MusitSecureAction().async { implicit request =>
      implicit val currUser: AuthenticatedUser = request.user

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
      implicit val currUser: AuthenticatedUser = request.user

      val jsr = request.body.validate[SaveAnalysisEventCommand]

      saveRequest[SaveAnalysisEventCommand, Option[AnalysisModuleEvent]](jsr)(
        sc => analysisService.add(mid, sc)
      )
    }

  def addResult(mid: MuseumId, id: Long) =
    MusitSecureAction().async(parse.json) { implicit request =>
      implicit val currUser: AuthenticatedUser = request.user
      val jsr                                  = request.body.validate[AnalysisResult]

      saveRequest[AnalysisResult, EventId](jsr) { r =>
        analysisService.addResult(mid, id, r)
      }
    }

  def updateResult(mid: MuseumId, eid: Long) =
    MusitSecureAction().async(parse.json) { implicit request =>
      implicit val currUser: AuthenticatedUser = request.user
      val jsr                                  = request.body.validate[AnalysisResult]

      updateRequest[AnalysisResult, EventId](jsr) { r =>
        analysisService.updateResult(mid, eid, r)
      }
    }

  def importResults(mid: MuseumId, eid: Long) =
    MusitSecureAction().async(parse.json) { implicit request =>
      implicit val currUser: AuthenticatedUser = request.user

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
    MusitSecureAction().async(parse.json) { implicit request =>
      implicit val currUser: AuthenticatedUser = implicitly(request.user)
      val jsr                                  = request.body.validate[SaveAnalysisEventCommand]

      updateRequestOpt[SaveAnalysisEventCommand, AnalysisEvent](jsr) { sc =>
        analysisService.update(mid, eid, sc)
      }
    }

  def getAnalysisEvents(mid: MuseumId, collectionIds: String) =
    MusitSecureAction().async { implicit request =>
      implicit val currUser: AuthenticatedUser = request.user

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

  def search(
      mid: Int,
      collectionIds: String,
      q: Option[String],
      types: String = "",
      from: Int,
      limit: Int
  ) =
    MusitSecureAction(mid, Read).async { implicit request =>
      implicit val currUser: AuthenticatedUser = request.user

      parseCollectionIdsParam(mid, collectionIds) match {
        case Left(res) => Future.successful(res)
        case Right(cids) =>
          analysisSearchService
            .restrictedAnalysisSearch(
              mid = MuseumId(mid),
              collectionIds = cids,
              from = from,
              limit = calcLimit(limit),
              queryStr = q,
              types = types.split(",")
            )
            .map {
              case MusitSuccess(res) =>
                Ok(res.raw)

              case err: MusitError =>
                logger.error(err.message)
                internalErr(err)
            }
      }
    }

}
