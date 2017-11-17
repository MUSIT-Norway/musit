package no.uio.musit.security

import play.api.libs.json._

sealed abstract class ModuleConstraint(val id: Int, val name: String)

case object AccessAll            extends ModuleConstraint(0, "All")
case object StorageFacility      extends ModuleConstraint(1, "Storage Facility")
case object CollectionManagement extends ModuleConstraint(2, "Collection Management")
case object DocumentArchive      extends ModuleConstraint(3, "Document Archive")

object ModuleConstraint {

  // Explicitly not including the {{{All}}} constraint
  val AllModules: Seq[ModuleConstraint] = Seq(
    CollectionManagement,
    StorageFacility,
    DocumentArchive
  )

  implicit val reads: Reads[ModuleConstraint] = Reads { jsv =>
    jsv.validate[Int] match {
      case JsSuccess(id, path) =>
        fromInt(id).map(JsSuccess(_)).getOrElse(JsError(path, s"Unknown Module ID $id"))

      case err: JsError =>
        err
    }
  }

  implicit val writes: Writes[ModuleConstraint] = Writes { m =>
    JsNumber(m.id)
  }

  @throws(classOf[NoSuchElementException])
  def unsafeFromInt(id: Int): ModuleConstraint = fromInt(id).get

  def fromInt(id: Int): Option[ModuleConstraint] = id match {
    case AccessAll.id            => Some(AccessAll)
    case StorageFacility.id      => Some(StorageFacility)
    case CollectionManagement.id => Some(CollectionManagement)
    case DocumentArchive.id      => Some(DocumentArchive)
    case _                       => None
  }

}
