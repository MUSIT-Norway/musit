package models

import play.api.libs.json.Json

case class MuseumIdentifier(museumNo: String, subNo: Option[String])

object MuseumIdentifier {
  def fromSqlString(displayId: String): MuseumIdentifier =
    displayId.split("/",2) match {
      case Array(museumNo, subNo) =>
        MuseumIdentifier(museumNo, Some(subNo))
      case Array(museumNo) =>
        MuseumIdentifier(museumNo, None)
    }

  implicit val format = Json.format[MuseumIdentifier]

}
