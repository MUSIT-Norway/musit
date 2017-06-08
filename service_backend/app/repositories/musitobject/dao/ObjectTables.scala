package repositories.musitobject.dao

import no.uio.musit.models.MuseumCollections.Collection
import no.uio.musit.models._
import play.api.db.slick.HasDatabaseConfigProvider
import repositories.shared.dao.ColumnTypeMappers
import slick.jdbc.JdbcProfile

trait ObjectTables
    extends HasDatabaseConfigProvider[JdbcProfile]
    with ColumnTypeMappers {

  import profile.api._

  // Type aliases representing rows for the different tables
  // format: off
  // scalastyle:off line.size.limit
  type ObjectRow = ((Option[ObjectId], Option[ObjectUUID], MuseumId, String, Option[Long], Option[String], Option[Long], Option[Long], Boolean, String, Option[String], Option[Long], Option[Collection], Option[String], Option[String], Option[String], Option[String], Option[String]))
  type MaterialRow = ((Option[Int], Option[Long], Option[String], Option[String], Option[String], Option[Int], Option[String], Option[String], Option[Int], Option[Long], Option[String], Option[Int]))
  type LocationRow = ((Option[Int], Option[Long], Option[String], Option[Int], Option[String], Option[Int], Option[String], Option[String],Option[String], Option[String],Option[String], Option[String],Option[String],Option[String], Option[String],Option[String], Option[String],Option[String],Option[Int], Option[Int]))
  type CoordinateRow = ((Option[Int], Option[Long], Option[String], Option[String], Option[String], Option[String]))
  // format: on
  // scalastyle:on line.size.limit

  val objTable             = TableQuery[ObjectTable]
  val thingMaterialTable   = TableQuery[ThingMaterialTable]
  val thingLocationTable   = TableQuery[ThingLocationTable]
  val thingCoordinateTable = TableQuery[ThingCoordinateTable]

  /**
   * Definition for the MUSIT_MAPPING.MUSITTHING table
   */
  class ObjectTable(
      val tag: Tag
  ) extends Table[ObjectRow](tag, Some("MUSIT_MAPPING"), "MUSITTHING") {

    // scalastyle:off method.name
    def * = (
      id.?,
      uuid,
      museumId,
      museumNo,
      museumNoAsNumber,
      subNo,
      subNoAsNumber,
      mainObjectId,
      isDeleted,
      term,
      oldSchema,
      oldObjId,
      newCollectionId,
      arkForm,
      arkFindingNo,
      natStage,
      natGender,
      natLegDate
    )

    // scalastyle:on method.name

    val id               = column[ObjectId]("OBJECT_ID", O.PrimaryKey, O.AutoInc)
    val uuid             = column[Option[ObjectUUID]]("MUSITTHING_UUID")
    val museumId         = column[MuseumId]("MUSEUMID")
    val museumNo         = column[String]("MUSEUMNO")
    val museumNoAsNumber = column[Option[Long]]("MUSEUMNOASNUMBER")
    val subNo            = column[Option[String]]("SUBNO")
    val subNoAsNumber    = column[Option[Long]]("SUBNOASNUMBER")
    val mainObjectId     = column[Option[Long]]("MAINOBJECT_ID")
    val isDeleted        = column[Boolean]("IS_DELETED")
    val term             = column[String]("TERM")
    val oldSchema        = column[Option[String]]("OLD_SCHEMANAME")
    val oldObjId         = column[Option[Long]]("LOKAL_PK")
    val oldBarcode       = column[Option[Long]]("OLD_BARCODE")
    val newCollectionId  = column[Option[Collection]]("NEW_COLLECTION_ID")
    val arkForm          = column[Option[String]]("ARK_FORM")
    val arkFindingNo     = column[Option[String]]("ARK_FUNN_NR")
    val natStage         = column[Option[String]]("NAT_STAGE")
    val natGender        = column[Option[String]]("NAT_GENDER")
    val natLegDate       = column[Option[String]]("NAT_LEGDATO")
  }

  /**
   * Definition for the MUSIT_MAPPING.THING_MATERIAL table
   */
  class ThingMaterialTable(
      val tag: Tag
  ) extends Table[MaterialRow](tag, Some("MUSIT_MAPPING"), "THING_MATERIAL") {

    // scalastyle:off method.name
    def * = (
      collectionid,
      objectid,
      etnMaterialtype,
      etnMaterial,
      etnMaterialElement,
      etnMatridLocal,
      arkMaterial,
      arkSpesMaterial,
      arkSorting,
      arkHidLocal,
      numMaterial,
      numNumistypeid
    )

    // scalastyle:on method.name
    val collectionid       = column[Option[Int]]("COLLECTIONID")
    val objectid           = column[Option[Long]]("OBJECTID")
    val etnMaterialtype    = column[Option[String]]("ETN_MATERIALTYPE")
    val etnMaterial        = column[Option[String]]("ETN_MATERIAL")
    val etnMaterialElement = column[Option[String]]("ETN_MATERIAL_ELEMENT")
    val etnMatridLocal     = column[Option[Int]]("ETN_MATRID_LOCAL")
    val arkMaterial        = column[Option[String]]("ARK_MATERIAL")
    val arkSpesMaterial    = column[Option[String]]("ARK_SPES_MATERIAL")
    val arkSorting         = column[Option[Int]]("ARK_SORTERING")
    val arkHidLocal        = column[Option[Long]]("ARK_HID_LOCAL")
    val numMaterial        = column[Option[String]]("NUM_MATERIAL")
    val numNumistypeid     = column[Option[Int]]("NUM_NUMISTYPEID")
    // scalastyle:on line.size.limit
  }

  /**
   * Definition for the MUSIT_MAPPING.THING_LOCATION table
   */
  class ThingLocationTable(
      val tag: Tag
  ) extends Table[LocationRow](tag, Some("MUSIT_MAPPING"), "THING_LOCATION") {

    // scalastyle:off method.name
    def * = (
      collectionid,
      objectid,
      arkFarm,
      arkFarmNo,
      arkBrukNo,
      arkLocalPlaceId,
      natCountry,
      natStateProvince,
      natMunicipality,
      natLocality,
      natCoordinate,
      natCoordDatum,
      natSoneBand,
      etnPlace,
      etnCountry,
      etnRegion1,
      etnRegion2,
      etnArea,
      etnLocalPlaceId,
      etnPlaceCount
    )

    // scalastyle:on method.name
    val collectionid     = column[Option[Int]]("COLLECTIONID")
    val objectid         = column[Option[Long]]("OBJECTID")
    val arkFarm          = column[Option[String]]("ARK_GARDSNAVN")
    val arkFarmNo        = column[Option[Int]]("ARK_GARDSNR")
    val arkBrukNo        = column[Option[String]]("ARK_BRUKSNR")
    val arkLocalPlaceId  = column[Option[Int]]("ARK_STEDID")
    val natCountry       = column[Option[String]]("NAT_COUNTRY")
    val natStateProvince = column[Option[String]]("NAT_STATE_PROVINCE")
    val natMunicipality  = column[Option[String]]("NAT_MUNICIPALITY")
    val natLocality      = column[Option[String]]("NAT_LOCALITY")
    val natCoordinate    = column[Option[String]]("NAT_COORDINATE")
    val natCoordDatum    = column[Option[String]]("NAT_COORD_DATUM")
    val natSoneBand      = column[Option[String]]("NAT_SONE_BAND")
    val etnPlace         = column[Option[String]]("ETN_PLACE")
    val etnCountry       = column[Option[String]]("ETN_COUNTRY")
    val etnRegion1       = column[Option[String]]("ETN_REGION1")
    val etnRegion2       = column[Option[String]]("ETN_REGION2")
    val etnArea          = column[Option[String]]("ETN_AREA")
    val etnLocalPlaceId  = column[Option[Int]]("ETN_LOCAL_STEDID")
    val etnPlaceCount    = column[Option[Int]]("ETN_PLACE_COUNT")

    // scalastyle:on line.size.limit
  }

  class ThingCoordinateTable(
      val tag: Tag
  ) extends Table[CoordinateRow](tag, Some("MUSIT_MAPPING"), "THING_COORDINATE") {

    // scalastyle:off method.name
    def * = (
      collectionid,
      objectid,
      arkProjection,
      arkPresision,
      arkNorth,
      arkEast
    )

    // scalastyle:on method.name
    val collectionid  = column[Option[Int]]("COLLECTIONID")
    val objectid      = column[Option[Long]]("OBJECTID")
    val arkProjection = column[Option[String]]("ARK_PROJEKSJON")
    val arkPresision  = column[Option[String]]("ARK_PRESISJON")
    val arkNorth      = column[Option[String]]("ARK_NORD")
    val arkEast       = column[Option[String]]("ARK_OST")

    // scalastyle:on line.size.limit
  }

}
