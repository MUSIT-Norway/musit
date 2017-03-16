
package models.loan

import enumeratum._
import play.api.libs.json.{Json, Reads, Writes, __}

sealed abstract class LoanType(val id: Long, val name: String) extends EnumEntry {
  override def entryName: String = name
}

object LoanType {
  implicit val reads: Reads[LoanType] = __.read[Long].map(LoanEventTypes.unsafeFromId)
  implicit val writes: Writes[LoanType] = Writes { typ =>
    Json.toJson[Long](typ.id)
  }
}

/**
 * Enumerations of the loan types
 */
object LoanEventTypes extends Enum[LoanType] {

  override def values = findValues

  def fromId(id: Long): Option[LoanType] = values.find(_.id == id)

  def unsafeFromId(id: Long): LoanType = fromId(id).get

  case object LentObjectsType extends LoanType(2, "LentObjects")

  case object ReturnedObjectsType extends LoanType(3, "ReturnedObjects")

}
