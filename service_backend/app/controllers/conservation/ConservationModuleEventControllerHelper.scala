package controllers.conservation

import models.conservation.events.ConservationEvent
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess, MusitValidationError}
import no.uio.musit.models.EventTypeId
import services.conservation.{ConservationEventService, ConservationProcessService}

sealed trait ConservationModuleEventControllerHelper {
  ///The eventType id that this service handles
  def eventTypeId: EventTypeId
}

trait ConservationProcessControllerHelper
    extends ConservationModuleEventControllerHelper {
  def eventService: ConservationProcessService
}

trait ConservationEventControllerHelper extends ConservationModuleEventControllerHelper {
  def eventService: ConservationEventService[ConservationEvent]
}

/** Will likely be needed for iteration over children events, to be able to get an upsert-action for each subevent.
 (Could be irrelevant if all ConservationEvents upserts in the same way, but if we want flexibility to have separate
 handling for some of the event types, we need something like this)
 */
trait SubEventHelper {
  def getConservationEventServiceFor(
      eventTypeId: EventTypeId
  ): Option[ConservationEventService[ConservationEvent]]

  def getConservationEventServiceElseValidationError(
      eventTypeId: EventTypeId
  ): MusitResult[ConservationEventService[ConservationEvent]] = {

    MusitResultUtils.optionToMusitResult(
      getConservationEventServiceFor(eventTypeId),
      MusitValidationError(s"Unable to find eventTypeId with value: $eventTypeId")
    )

  }
}
