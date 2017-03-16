package models.analysis

/**
 * Contains information about what is being sent to a destination. E.g. a
 * Lab that performs analysis on objects. Which objects/samples are being sent.
 * And which analysis' are to be performed on the items in the shipment.
 */
case class Shipment[A](
    to: String,
    description: Option[String],
    content: Seq[A]
// TODO: Proabably need to add some Actor information.
)
