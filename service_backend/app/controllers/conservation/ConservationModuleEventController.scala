package controllers.conservation

import com.google.inject.{Inject, Singleton}
import controllers.{internalErr, saveRequest, updateRequestOpt}
import models.conservation.events._
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
) extends MusitController {

  val logger = Logger(classOf[ConservationController])

  // ----  Some local helpers  ----------------

  //Todo: some type-cleaning ought to be done here, it's ugly (and redundant) with these asInstanceOf calls,
  private def getConservationEventService(
      conservationEventTypeId: EventTypeId
  ): Option[ConservationEventService[ConservationEvent]] = {
    ConservationEventType(conservationEventTypeId).map {
      case Treatment =>
        treatmentController.service
          .asInstanceOf[ConservationEventService[ConservationEvent]]
      case TechnicalDescription =>
        technicalDescriptionController.service
          .asInstanceOf[ConservationEventService[ConservationEvent]]
    }
  }

  private def getEventTypeIdInJson(json: JsValue): MusitResult[EventTypeId] = {
    (json \ "eventTypeId").validate[EventTypeId] match {
      case JsSuccess(eventTypeId, path) =>
        MusitSuccess(eventTypeId)

      case err: JsError =>
        MusitValidationError("Missing eventTypeId")
    }
  }

  private def findConservationEventServiceBlameClientIfNotFound(
      conservationEventId: EventTypeId
  ) = {
    MusitResultUtils.optionToMusitResult(
      getConservationEventService(conservationEventId),
      MusitValidationError(s"Undefined eventTypeId: $conservationEventId")
    )
  }

  // -------------- Here the real stuff begins ------------------------------------------

  implicit val reads  = models.conservation.events.ConservationProcess.reads
  implicit val writes = models.conservation.events.ConservationProcess.writes

  import MusitResultUtils._

  def addEvent(mid: MuseumId) =
    MusitSecureAction(mid, CollectionManagement, Write).async(parse.json) {
      implicit request =>
        implicit val currUser = request.user

        val jsonBody      = request.body
        val mrEventTypeId = getEventTypeIdInJson(jsonBody)

        mrEventTypeId.flatMapToFutureResult {
          case ConservationProcess.eventTypeId => {
            val jsr = jsonBody.validate[ConservationProcess]

            saveRequest[ConservationModuleEvent, Option[ConservationModuleEvent]](jsr) {
              case event: ConservationProcess =>
                consService.add(mid, event)
              case wrong =>
                Future.successful(
                  MusitValidationError(
                    s"Expected ConservationProcess Event)"
                  )
                )
            }
          }

          case conservationEventId => {
            implicit val readsEvent = models.conservation.events.ConservationEvent.reads
            val jsr                 = jsonBody.validate[ConservationEvent]

            val mrService =
              findConservationEventServiceBlameClientIfNotFound(conservationEventId)

            mrService.flatMapToFutureResult { service =>
              saveRequest[ConservationEvent, Option[ConservationEvent]](jsr) {
                case event: ConservationEvent =>
                  service.add(mid, event)
                case wrong =>
                  Future.successful(
                    MusitValidationError(
                      s"Expected ConservationEvent with eventTypeId: $conservationEventId"
                    )
                  )
              }
            }
          }
        }
    }

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

    val futMrOptEvent =
      MusitResultUtils
        .futureMusitResultFlatMap[EventTypeId, Option[ConservationModuleEvent]](
          futMrEventTypeId, {
            case ConservationProcess.eventTypeId =>
              consService.findConservationProcessById(mid, id)
            case conservationEventId => {
              val mrService =
                findConservationEventServiceBlameClientIfNotFound(conservationEventId)
              mrService.flatMapToFutureMusitResult(
                eventService => eventService.findConservationEventById(mid, id)
              )
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

        val jsonBody = request.body

        val mrEventTypeId = getEventTypeIdInJson(jsonBody)

        mrEventTypeId.flatMapToFutureResult {
          case ConservationProcess.eventTypeId => {
            val jsr = jsonBody.validate[ConservationProcess]
            updateRequestOpt[ConservationProcess, ConservationProcess](jsr) { cp =>
              consService.update(mid, eventId, cp)
            }
          }
          case conservationEventId => {
            implicit val readsEvent = ConservationEvent.reads
            val jsr                 = jsonBody.validate[ConservationEvent]

            val mrService =
              findConservationEventServiceBlameClientIfNotFound(conservationEventId)

            mrService.flatMapToFutureResult { eventService =>
              updateRequestOpt[ConservationEvent, ConservationEvent](jsr) { cp =>
                eventService.update(mid, eventId, cp)
              }
            }
          }
        }
    }
}
