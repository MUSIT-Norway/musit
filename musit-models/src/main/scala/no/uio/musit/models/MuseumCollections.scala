package no.uio.musit.models

import play.api.libs.json._

import CollectionUUID.unsafeFromString

object MuseumCollections {

  def fromJsonString(str: String): Seq[Collection] = {
    val js = Json.parse(str)
    Json
      .fromJson[Seq[Int]](js)
      .map { ints =>
        ints.map(Collection.fromInt).distinct
      }
      .getOrElse(Seq.empty)
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

  sealed trait Collection {
    val id: Int
    val uuid: CollectionUUID
    // This attribute indicates which DB schemas contain a reference to this
    // specific collection. Once we've migrated it all away from the old systems
    // the attribute (and all it's complexities) can be removed.
    val schemas: Seq[String]
  }

  object Collection {

    implicit val reads: Reads[Collection] = __.read[Int].map(fromInt)

    implicit val writes: Writes[Collection] = Writes(os => JsNumber(os.id))

    private[this] val InvalidMsg = (arg: String) =>
      String.format("%s can not be mapped to an old schema.", arg)

    // scalastyle:off
    @throws(classOf[IllegalArgumentException])
    def fromInt(i: Int): Collection = {
      i match {
        case Archeology.id          => Archeology
        case Ethnography.id         => Ethnography
        case Numismatics.id         => Numismatics
        case Lichen.id              => Lichen
        case Moss.id                => Moss
        case Fungi.id               => Fungi
        case Algae.id               => Algae
        case VascularPlants.id      => VascularPlants
        case Entomology.id          => Entomology
        case MarineInvertebrates.id => MarineInvertebrates
        case _                      => throw new IllegalArgumentException(InvalidMsg(i.toString))
      }
    }

    @throws(classOf[IllegalArgumentException])
    def fromString(str: String): Collection = {
      all.find(_.schemas.contains(str)).getOrElse {
        throw new IllegalArgumentException(InvalidMsg(str))
      }
    }

    def fromCollectionUUID(uuid: CollectionUUID): Collection = {
      uuid match {
        case Archeology.uuid          => Archeology
        case Ethnography.uuid         => Ethnography
        case Numismatics.uuid         => Numismatics
        case Lichen.uuid              => Lichen
        case Moss.uuid                => Moss
        case Fungi.uuid               => Fungi
        case Algae.uuid               => Algae
        case VascularPlants.uuid      => VascularPlants
        case Entomology.uuid          => Entomology
        case MarineInvertebrates.uuid => MarineInvertebrates
        case _                        => throw new IllegalArgumentException(InvalidMsg(uuid.asString))
      }
    }

    // scalastyle:on
  }

  sealed trait Culture
  sealed trait Nature

  case object Archeology extends Collection with Culture {
    override val id: Int = 1
    override val uuid    = unsafeFromString("2e4f2455-1b3b-4a04-80a1-ba92715ff613")
    override val schemas: Seq[String] = Seq(
      "USD_ARK_GJENSTAND_S",
      "USD_ARK_GJENSTAND_B",
      "USD_ARK_GJENSTAND_O",
      "USD_ARK_GJENSTAND_NTNU",
      "USD_ARK_GJENSTAND_TROMSO"
    )
  }

  case object Ethnography extends Collection with Culture {
    override val id: Int = 2
    override val uuid    = unsafeFromString("88b35138-24b5-4e62-bae4-de80fae7df82")
    override val schemas: Seq[String] = Seq(
      "USD_ETNO_GJENSTAND_B",
      "USD_ETNO_GJENSTAND_O",
      "USD_ETNO_GJENSTAND_TROMSO"
    )
  }

  case object Numismatics extends Collection with Culture {
    override val id: Int              = 3
    override val uuid                 = unsafeFromString("8bbdf9b3-56d1-479a-9509-2ea82842e8f8")
    override val schemas: Seq[String] = Seq("USD_NUMISMATIKK")
  }

  case object Lichen extends Collection with Nature {
    override val id: Int              = 4
    override val uuid                 = unsafeFromString("fcb4c598-8b05-4095-ac00-ce66247be38a")
    override val schemas: Seq[String] = Seq("MUSIT_BOTANIKK_LAV")
  }

  case object Moss extends Collection with Nature {
    override val id: Int              = 5
    override val uuid                 = unsafeFromString("d0dd5ad3-c22f-4ea0-8b52-dc5b0e17aa24")
    override val schemas: Seq[String] = Seq("MUSIT_BOTANIKK_MOSE")
  }

  case object Fungi extends Collection with Nature {
    override val id: Int              = 6
    override val uuid                 = unsafeFromString("23ca0166-5f9e-44c2-ab0d-b4cdd704af07")
    override val schemas: Seq[String] = Seq("MUSIT_BOTANIKK_SOPP")
  }

  case object Algae extends Collection with Nature {
    override val id: Int              = 7
    override val uuid                 = unsafeFromString("1d8dd4e6-1527-439c-ac86-fc315e0ce852")
    override val schemas: Seq[String] = Seq("MUSIT_BOTANIKK_ALGE")
  }

  case object VascularPlants extends Collection with Nature {
    override val id: Int              = 8
    override val uuid                 = unsafeFromString("7352794d-4973-447b-b84e-2635cafe910a")
    override val schemas: Seq[String] = Seq("MUSIT_BOTANIKK_FELLES")
  }

  case object Entomology extends Collection with Nature {
    override val id: Int              = 9
    override val uuid                 = unsafeFromString("ba3d4d30-810b-4c07-81b3-37751f2196f0")
    override val schemas: Seq[String] = Seq("MUSIT_BOTANIKK_ENTOMOLOGI")
  }

  case object MarineInvertebrates extends Collection with Nature {
    override val id: Int              = 10
    override val uuid                 = unsafeFromString("ef4dc066-b6f8-4155-89f8-7aa9aeeb2dc4")
    override val schemas: Seq[String] = Seq("MUSIT_BOTANIKK_ENTOMOLOGI")
  }

}
