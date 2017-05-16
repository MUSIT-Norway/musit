package models.geolocation

import play.api.libs.json.{Json, Reads}

case class GeoNorwayAddress(
    `type`: String,
    adressenavn: String,
    husnr: Option[String],
    bokstav: Option[String],
    poststed: Option[String],
    postnr: Option[String]
)

object GeoNorwayAddress {

  implicit val reads: Reads[GeoNorwayAddress] = Json.reads[GeoNorwayAddress]

  def asAddress(gna: GeoNorwayAddress): Address = {
    val houseLetter = gna.bokstav.map(b => " " + b).getOrElse("")
    val streetNum   = gna.husnr.map(_ + houseLetter)
    val zipCode     = gna.postnr.map(pnr => if (pnr.length < 4) s"0$pnr" else pnr)

    Address(
      street = gna.adressenavn,
      streetNo = streetNum.getOrElse(""),
      place = gna.poststed.getOrElse(""),
      zip = zipCode.getOrElse("")
    )
  }

}
