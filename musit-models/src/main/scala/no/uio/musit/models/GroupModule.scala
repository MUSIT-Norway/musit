package no.uio.musit.models

import play.api.libs.json._

sealed trait GroupModule {
  val id: Int
}

case object StorageFacility extends GroupModule { val id = 1 }
case object Loan extends GroupModule { val id = 2 }
case object Analysis extends GroupModule { val id = 3 }

object GroupModule {

  implicit val reads: Reads[GroupModule] = Reads { jsv =>
    jsv.validate[Int] match {
      case JsSuccess(id, path) =>
        fromInt(id).map(JsSuccess(_)).getOrElse(JsError(path, s"Unknown Module ID $id"))

      case err: JsError =>
        err
    }
  }

  implicit val writes: Writes[GroupModule] = Writes{ m => JsNumber(m.id)}

  @throws(classOf[NoSuchElementException])
  def unsafeFromInt(id: Int): GroupModule = fromInt(id).get

  def fromInt(id: Int): Option[GroupModule] = id match {
    case StorageFacility.id => Some(StorageFacility)
    case Loan.id => Some(Loan)
    case _ => None
  }

}
