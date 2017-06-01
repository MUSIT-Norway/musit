package no.uio.musit.models

import no.uio.musit.models.MuseumCollections.Collection
import play.api.libs.json.{Format, Json}

case class MuseumCollection(
    uuid: CollectionUUID,
    name: Option[String],
    oldSchemaNames: Seq[Collection]
) {

  def schemaIds: Seq[Int] = oldSchemaNames.map(_.id).distinct

  def collection = Collection.fromCollectionUUID(uuid)

}

object MuseumCollection {

  implicit val format: Format[MuseumCollection] = Json.format[MuseumCollection]

  def fromTuple(t: (CollectionUUID, Option[String], Seq[Collection])): MuseumCollection = {
    MuseumCollection(
      uuid = t._1,
      name = t._2,
      oldSchemaNames = t._3
    )
  }

}
