package models.conservation

import no.uio.musit.models.EventTypeId

package object events {

  type ConservationTypeId = EventTypeId

  def ConservationTypeId(value: Int) = EventTypeId(value)
}
