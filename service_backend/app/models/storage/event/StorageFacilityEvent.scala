package models.storage.event

import no.uio.musit.models.MusitEvent

trait StorageFacilityEvent extends MusitEvent { self =>

  val eventType: StorageFacilityEventType

}
