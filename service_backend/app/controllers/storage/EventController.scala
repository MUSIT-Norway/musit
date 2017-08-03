package controllers.storage

import com.google.inject.{Inject, Singleton}
import controllers.invaludUuidResponse
import models.storage.event.control.Control
import models.storage.event.observation.Observation
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.models.StorageNodeId
import no.uio.musit.security.Authenticator
import no.uio.musit.security.Permissions._
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import services.storage.{ControlService, ObservationService}

import scala.concurrent.Future

@Singleton
class EventController @Inject()(
    val authService: Authenticator,
    val controlService: ControlService,
    val observationService: ObservationService
) extends MusitController {

  val logger = Logger(classOf[EventController])

  /**
   * Controller endpoint for adding a new Control for a storage node with
   * the given nodeId.
   */
  def addControl(
      mid: Int,
      nodeId: String
  ) = MusitSecureAction(mid, Write).async(parse.json) { implicit request =>
    implicit val currUser = request.user

    StorageNodeId
      .fromString(nodeId)
      .map { nid =>
        request.body.validate[Control] match {
          case JsSuccess(ctrl, jsPath) =>
            controlService.add(mid, nid, ctrl).map {
              case MusitSuccess(addedCtrl) =>
                Created(Json.toJson(addedCtrl))

              case err: MusitError =>
                InternalServerError(Json.obj("message" -> err.message))
            }
          case JsError(errors) =>
            Future.successful(BadRequest(JsError.toJson(errors)))
        }
      }
      .getOrElse(invaludUuidResponse(nodeId))
  }

  /**
   * Controller endpoint for adding a new Observation for a storage node with
   * the given nodeId.
   */
  def addObservation(
      mid: Int,
      nodeId: String
  ) = MusitSecureAction(mid, Write).async(parse.json) { implicit request =>
    implicit val currUser = request.user

    StorageNodeId
      .fromString(nodeId)
      .map { nid =>
        request.body.validate[Observation] match {
          case JsSuccess(obs, jsPath) =>
            observationService.add(mid, nid, obs).map {
              case MusitSuccess(addedObs) =>
                Created(Json.toJson(addedObs))

              case err: MusitError =>
                InternalServerError(Json.obj("message" -> err.message))
            }
          case JsError(errors) =>
            Future.successful(BadRequest(JsError.toJson(errors)))
        }
      }
      .getOrElse(invaludUuidResponse(nodeId))
  }

  /**
   * Fetch a Control with the given eventId for a storage node where the id is
   * equal to the provided nodeId.
   */
  def getControl(
      mid: Int,
      nodeId: String,
      eventId: Long
  ) = MusitSecureAction(mid, Read).async { implicit request =>
    implicit val currUser = request.user

    StorageNodeId
      .fromString(nodeId)
      .map { nid =>
        controlService.findBy(mid, eventId).map {
          case MusitSuccess(maybeControl) =>
            maybeControl.map { ctrl =>
              Ok(Json.toJson(ctrl))
            }.getOrElse {
              NotFound
            }

          case err: MusitError =>
            InternalServerError(Json.obj("message" -> err.message))
        }
      }
      .getOrElse(invaludUuidResponse(nodeId))
  }

  /**
   * Fetch an Observation with the given eventId for a storage node where the
   * id is equal to the provided nodeId.
   */
  def getObservation(
      mid: Int,
      nodeId: String,
      eventId: Long
  ) = MusitSecureAction(mid, Read).async { implicit request =>
    implicit val currUser = request.user

    StorageNodeId
      .fromString(nodeId)
      .map { nid =>
        observationService.findBy(mid, eventId).map {
          case MusitSuccess(maybeObservation) =>
            maybeObservation.map { obs =>
              Ok(Json.toJson(obs))
            }.getOrElse {
              NotFound
            }
          case err: MusitError =>
            InternalServerError(Json.obj("message" -> err.message))
        }
      }
      .getOrElse(invaludUuidResponse(nodeId))
  }

  /**
   * Lists all Controls for the given nodeId
   */
  def listControls(
      mid: Int,
      nodeId: String
  ) = MusitSecureAction(mid, Read).async { implicit request =>
    implicit val currUser = request.user

    StorageNodeId
      .fromString(nodeId)
      .map { nid =>
        controlService.listFor(mid, nid).map {
          case MusitSuccess(controls) =>
            Ok(Json.toJson(controls))

          case err: MusitError =>
            InternalServerError(Json.obj("message" -> err.message))
        }
      }
      .getOrElse(invaludUuidResponse(nodeId))
  }

  /**
   * Lists all Observations for the given nodeId
   */
  def listObservations(
      mid: Int,
      nodeId: String
  ) = MusitSecureAction(mid, Read).async { implicit request =>
    implicit val currUser = request.user

    StorageNodeId
      .fromString(nodeId)
      .map { nid =>
        observationService.listFor(mid, nid).map {
          case MusitSuccess(observations) =>
            Ok(Json.toJson(observations))

          case err: MusitError =>
            InternalServerError(Json.obj("message" -> err.message))
        }
      }
      .getOrElse(invaludUuidResponse(nodeId))
  }

  /**
   * Returns a mixed list of controls and observations for a storage node with
   * the given nodeId.
   */
  def listEventsForNode(
      mid: Int,
      nodeId: String
  ) = MusitSecureAction(mid, Read).async { implicit request =>
    implicit val currUser = request.user

    StorageNodeId
      .fromString(nodeId)
      .map { nid =>
        val eventuallyCtrls = controlService.listFor(mid, nid)
        val eventyallyObs   = observationService.listFor(mid, nid)
        for {
          ctrlRes <- eventuallyCtrls
          obsRes  <- eventyallyObs
        } yield {
          val sortedRes = for {
            controls     <- ctrlRes
            observations <- obsRes
          } yield {
            controls
              .union(observations)
              // Sorting by doneDate. If None place last in list.
              .sortBy(_.doneDate.map(_.getMillis).getOrElse(Long.MaxValue))
          }

          sortedRes match {
            case MusitSuccess(sorted) =>
              val jsObjects = sorted.map {
                case ctrl: Control    => Json.toJson(ctrl)
                case obs: Observation => Json.toJson(obs)
                case _                => JsNull
              }
              logger.debug(
                s"Going to return sorted JSON:" +
                  s"\n${Json.prettyPrint(JsArray(jsObjects))}"
              )
              Ok(JsArray(jsObjects))

            case err: MusitError =>
              InternalServerError(Json.obj("message" -> err.message))
          }

        }
      }
      .getOrElse(invaludUuidResponse(nodeId))
  }

}
