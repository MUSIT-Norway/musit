package models.conservation.events

import models.conservation.events.ConservationExtras.{
  DescriptionAttributeValue,
  ExtraAttributes
}
import play.api.libs.json.{JsObject, Json, Reads, Writes}

object ConservationExtras {

  /**
   * ExtraAttributes are used to provide additional fields to a specific
   * {{{Conservation}}} type. Each {{{ExtraAttributes}}} implementation can have
   * one or more attribute fields. And they can contain a type specific set of
   * allowed values. These values are implemented as ADT's in the companion
   * object for the specific {{{ExtraAttributes}}} implementation.
   *
   * The allowed values of the ADT typed attributes need to be provided as part
   * of the response, when the client fetches the list of {{{ConservationType}}}s.
   */
  sealed trait ExtraAttributes

  trait ExtraAttributesOps {
    val typeName: String
    val allValues: Seq[DescriptionAttributeValue] = Seq.empty
  }

  trait DescriptionAttributeValue {
    val id: Int
    val enLabel: String
    val noLabel: String
  }
}

object ExtraAttributes {

  private val discriminator = "type"
}
