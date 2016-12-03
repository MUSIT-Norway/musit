/*
 * MUSIT is a museum database to archive natural and cultural history data.
 * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package no.uio.musit.models

import play.api.libs.json._

object OldDbSchemas {

  def fromJsonString(str: String): Seq[OldSchema] = {
    val js = Json.parse(str)
    Json.fromJson[Seq[Int]](js).map { ints =>
      ints.map(OldSchema.fromInt).distinct
    }.getOrElse(Seq.empty)
  }

  val all = Seq(
    Archeology,
    Ethnography,
    Numismatics,
    Lichen,
    Moss,
    Fungi,
    Algae,
    VascularPlants,
    Entomology,
    MarineInvertebrates
  )

  sealed trait OldSchema {
    val id: Int
    val schemas: Seq[String]
  }

  object OldSchema {

    implicit val reads: Reads[OldSchema] = __.read[Int].map(fromInt)

    implicit val writes: Writes[OldSchema] = Writes(os => JsNumber(os.id))

    // scalastyle:off
    @throws(classOf[IllegalArgumentException])
    def fromInt(i: Int): OldSchema = {
      i match {
        case Archeology.id => Archeology
        case Ethnography.id => Ethnography
        case Numismatics.id => Numismatics
        case Lichen.id => Lichen
        case Moss.id => Moss
        case Fungi.id => Fungi
        case Algae.id => Algae
        case VascularPlants.id => VascularPlants
        case Entomology.id => Entomology
        case MarineInvertebrates.id => MarineInvertebrates
        case _ => throw new IllegalArgumentException("") // scalastyle:ignore
      }
    }

    // scalastyle:on
  }

  case object Archeology extends OldSchema {
    override val id: Int = 1
    override val schemas: Seq[String] = Seq(
      "USD_ARK_GJENSTAND_S",
      "USD_ARK_GJENSTAND_B",
      "USD_ARK_GJENSTAND_O",
      "USD_ARK_GJENSTAND_NTNU",
      "USD_ARK_GJENSTAND_TROMSO"
    )
  }

  case object Ethnography extends OldSchema {
    override val id: Int = 2
    override val schemas: Seq[String] = Seq(
      "USD_ETNO_GJENSTAND_B",
      "USD_ETNO_GJENSTAND_O",
      "USD_ETNO_GJENSTAND_TROMSO"
    )
  }

  case object Numismatics extends OldSchema {
    override val id: Int = 3
    override val schemas: Seq[String] = Seq("USD_NUMISMATIKK")
  }

  case object Lichen extends OldSchema {
    override val id: Int = 4
    override val schemas: Seq[String] = Seq("MUSIT_BOTANIKK_LAV")
  }

  case object Moss extends OldSchema {
    override val id: Int = 5
    override val schemas: Seq[String] = Seq("MUSIT_BOTANIKK_MOSE")
  }

  case object Fungi extends OldSchema {
    override val id: Int = 6
    override val schemas: Seq[String] = Seq("MUSIT_BOTANIKK_SOPP")
  }

  case object Algae extends OldSchema {
    override val id: Int = 7
    override val schemas: Seq[String] = Seq("MUSIT_BOTANIKK_ALGE")
  }

  case object VascularPlants extends OldSchema {
    override val id: Int = 8
    override val schemas: Seq[String] = Seq("MUSIT_BOTANIKK_FELLES")
  }

  case object Entomology extends OldSchema {
    override val id: Int = 9
    override val schemas: Seq[String] = Seq("MUSIT_BOTANIKK_ENTOMOLOGI")
  }

  case object MarineInvertebrates extends OldSchema {
    override val id: Int = 10
    // TODO: There's some uncertainty about this one. Ask Svein...
    override val schemas: Seq[String] = Seq("MUSIT_BOTANIKK_ENTOMOLOGI")
  }

}
