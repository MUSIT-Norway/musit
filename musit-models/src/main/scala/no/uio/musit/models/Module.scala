package no.uio.musit.models

import play.api.libs.json.{JsNumber, Reads, Writes, __}

sealed trait Module {
  val id: Int
}

case object StorageFacility extends Module {
  val id = 1
}
case object LoanProcess extends Module {
  val id = 2
}

object Module {

  implicit val reads: Reads[Module]   = __.read[Int].map(fromInt)
  implicit val writes: Writes[Module] = Writes{m => JsNumber(m.id)}

  def fromInt(id: Int): Module = id match {
    case StorageFacility.id => StorageFacility
    case LoanProcess.id => LoanProcess
  }
}
