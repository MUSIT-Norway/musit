package controllers.conservation

import com.google.inject.{Inject, Singleton}
import controllers.{internalErr, saveRequest, updateRequestOpt}
import models.conservation.events.{
  ConservationEvent,
  ConservationModuleEvent,
  ConservationProcess
}
import no.uio.musit.MusitResults.{MusitError, MusitSuccess, _}
import no.uio.musit.models.{EventId, EventTypeId, MuseumId}
import no.uio.musit.security.Permissions.{Read, Write}
import no.uio.musit.security.{AuthenticatedUser, Authenticator, CollectionManagement}
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{ControllerComponents, Result}
import services.conservation.{ConservationProcessService, _}

import scala.concurrent.Future
@Singleton
class ConservationModuleEventController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val consService: ConservationProcessService,
    val conservationService: ConservationService,
    val conservationProcessController: ConservationProcessController,
    val treatmentController: TreatmentController,
    val technicalDescriptionController: TechnicalDescriptionController
) extends MusitController
    with SubEventHelper {

  val logger = Logger(classOf[ConservationController])

  /// The child event types as well as the conservation process event type
  val eventControllerHelpers: Array[ConservationModuleEventControllerHelper] =
    Array(
      conservationProcessController,
      treatmentController,
      technicalDescriptionController
    )

  // ----  Some local helpers  ----------------

  def findControllerHelperForEventTypeId(
      eventTypeId: EventTypeId
  ): Option[ConservationModuleEventControllerHelper] =
    eventControllerHelpers.find(_.eventTypeId == eventTypeId)

  def controllerHelperByEventTypeId(
      eventTypeId: EventTypeId,
      errorIfNotFound: => MusitError
  ): MusitResult[ConservationModuleEventControllerHelper] = {
    findControllerHelperForEventTypeId(eventTypeId) match {
      case Some(t) => MusitSuccess(t)
      case None    => errorIfNotFound
    }
  }

  //Implementation of SubEventHelper:
  def getConservationEventServiceFor(
      eventTypeId: EventTypeId
  ): Option[ConservationEventService[ConservationEvent]] = {
    val res = findControllerHelperForEventTypeId(eventTypeId)
    res.map {
      case subEventHelper: ConservationEventControllerHelper =>
        subEventHelper.eventService
      case processHelper: ConservationProcessControllerHelper =>
        throw new IllegalStateException(
          "Internal error, getControllerFor expected to find a subEventHelper, not the process helper"
        )
    }
  }

  def subEventHelper: SubEventHelper = this

  private def getEventTypeIdInJson(json: JsValue): MusitResult[Int] = {
    (json \ "eventTypeId").validate[Int] match {
      case JsSuccess(eventTypeId, path) =>
        MusitSuccess(eventTypeId)

      case err: JsError =>
        MusitValidationError("Missing eventTypeId")
    }
  }

  import MusitResultUtils.MusitResultHelpers

  private def getControllerHelperFromEventTypeIdInJson(
      jsonBody: JsValue
  ): MusitResult[ConservationModuleEventControllerHelper] = {
    getEventTypeIdInJson(jsonBody).flatMap { eventTypeId =>
      controllerHelperByEventTypeId(
        EventTypeId(eventTypeId),
        MusitValidationError("Unknown eventTypeId: " + eventTypeId)
      )
    }
  }

  private def getControllerFromEventTypeId(
      futMrEventTypeId: Future[MusitResult[EventTypeId]]
  ): Future[MusitResult[ConservationModuleEventControllerHelper]] = {
    futMrEventTypeId.map(
      mrEventTypeId =>
        mrEventTypeId.flatMap { eventTypeId =>
          controllerHelperByEventTypeId(
            eventTypeId,
            MusitValidationError("Unknown eventTypeId: " + eventTypeId)
          )
      }
    )
  }

  // -------------- Here the real stuff begins ------------------------------------------

  implicit val reads  = models.conservation.events.ConservationProcess.reads
  implicit val writes = models.conservation.events.ConservationProcess.writes

  def addEvent(mid: MuseumId) =
    MusitSecureAction(mid, CollectionManagement, Write).async(parse.json) {
      implicit request =>
        implicit val currUser = request.user
        val jsonBody          = request.body

        val mrControllerHelper = getControllerHelperFromEventTypeIdInJson(jsonBody)

        mrControllerHelper.flatMapToFutureResult {
          case processhelper: ConservationProcessControllerHelper => {
            val jsr = jsonBody.validate[ConservationProcess]

            saveRequest[ConservationModuleEvent, Option[ConservationModuleEvent]](jsr) {
              case event: ConservationProcess =>
                processhelper.eventService.add(mid, event, subEventHelper)
              case wrong =>
                Future.successful(
                  MusitValidationError(
                    s"Expected ProcessEvent (with eventTypeId: ${processhelper.eventTypeId})"
                  )
                )
            }
          }

          case controllerHelper: ConservationEventControllerHelper => {
            implicit val readsEvent = models.conservation.events.ConservationEvent.reads
            val jsr                 = jsonBody.validate[ConservationEvent]

            saveRequest[ConservationEvent, Option[ConservationEvent]](jsr) {
              case event: ConservationEvent =>
                controllerHelper.eventService.add(mid, event)
              case wrong =>
                Future.successful(
                  MusitValidationError(
                    s"Expected ConservationEvent with eventTypeId: ${controllerHelper.eventTypeId}"
                  )
                )
            }
          }
        }
    }

  import MusitResultUtils._

  def getEventById(mid: MuseumId, id: EventId) =
    MusitSecureAction(mid, CollectionManagement, Read).async { implicit request =>
      implicit val currUser = request.user

      innerGetEventById(mid, id)
    }

  def innerGetEventById(mid: MuseumId, id: EventId)(
      implicit currUser: AuthenticatedUser
  ): Future[Result] = {
    val futMrOptEventTypeId = conservationService.getEventTypeId(id)

    val futMrEventTypeId = futureMusitResultFoldNone(
      futMrOptEventTypeId,
      //Todo: This may be a client/validation error, if it passes in an invalided eventId
      MusitInternalError(s"Unable to find event type id for event with id: $id")
    )

    val futMrControllerHelper = getControllerFromEventTypeId(futMrEventTypeId)

    val futMrOptEvent =
      MusitResultUtils.futureMusitFlatMap[ConservationModuleEventControllerHelper, Option[
        ConservationModuleEvent
      ]](
        futMrControllerHelper, {
          controllerHelper: ConservationModuleEventControllerHelper =>
            controllerHelper match {
              case processHelper: ConservationProcessControllerHelper =>
                processHelper.eventService.findConservationProcessById(mid, id)
              case eventHelper: ConservationEventControllerHelper =>
                eventHelper.eventService.findConservationEventById(mid, id)
            }
        }
      )
    futMrOptEvent.map {
      case MusitSuccess(ma) => ma.map(ae => Ok(Json.toJson(ae))).getOrElse(NotFound)
      case err: MusitError  => internalErr(err)
    }
  }

  def updateEvent(mid: MuseumId, eventId: EventId) =
    MusitSecureAction(mid, CollectionManagement, Write).async(parse.json) {
      implicit request =>
        implicit val currUser = implicitly(request.user)

        val jsonBody           = request.body
        val mrControllerHelper = getControllerHelperFromEventTypeIdInJson(jsonBody)

        mrControllerHelper.flatMapToFutureResult {
          case processhelper: ConservationProcessControllerHelper => {
            val jsr = jsonBody.validate[ConservationProcess]
            updateRequestOpt[ConservationModuleEvent, ConservationProcess](jsr) { cp =>
              consService.update(mid, eventId, cp)
            }
          }
          case eventHelper: ConservationEventControllerHelper => {
            implicit val readsEvent = ConservationEvent.reads
            val jsr                 = jsonBody.validate[ConservationEvent]

            updateRequestOpt[ConservationEvent, ConservationEvent](jsr) { cp =>
              eventHelper.eventService.update(mid, eventId, cp)
            }
          }
        }
    }
}
