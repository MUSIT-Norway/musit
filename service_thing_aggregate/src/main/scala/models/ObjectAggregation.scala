package models

case class ObjectId(id: Long)

case class MuseumId(id: Long)

case class ObjectAggregation(id: ObjectId, name: String, museumId: MuseumId)
