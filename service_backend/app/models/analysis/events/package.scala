package models.analysis

import no.uio.musit.models.EventTypeId

package object events {

  type AnalysisTypeId = EventTypeId

  def AnalysisTypeId(value: Int) = EventTypeId(value)

}
