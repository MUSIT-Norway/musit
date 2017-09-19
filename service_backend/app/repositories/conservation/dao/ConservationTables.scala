package repositories.conservation.dao

import models.conservation.events.{ConservationType, ConservationTypeId}
import no.uio.musit.models.CollectionUUID
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.json.{JsValue, Json}
import repositories.shared.dao.ColumnTypeMappers
import slick.jdbc.JdbcProfile

/**
 * Defines the table definitions needed to work with analysis data.
 * Also includes a few helpful functions to convert between rows and types.
 */
trait ConservationTables
    extends HasDatabaseConfigProvider[JdbcProfile]
    with ColumnTypeMappers {

  import profile.api._

  val conservationTypeTable = TableQuery[ConservationTypeTable]

  // scalastyle:off line.size.limit
  type ConservationTypeRow =
    (
        ConservationTypeId,
        String,
        String,
        Option[String],
        Option[String],
        Option[JsValue]
    )

  /**
   * Representation of the MUSARK_CONSERVATION.EVENT_TYPE table
   */
  class ConservationTypeTable(
      val tag: Tag
  ) extends Table[ConservationTypeRow](
        tag,
        Some(SchemaName),
        ConservationEventTypeTableName
      ) {

    val typeId       = column[ConservationTypeId]("TYPE_ID", O.PrimaryKey, O.AutoInc)
    val noName       = column[String]("NO_NAME")
    val enName       = column[String]("EN_NAME")
    val collections  = column[Option[String]]("COLLECTIONS")
    val descAttrType = column[Option[String]]("EXTRA_DESCRIPTION_TYPE")
    val descAttrs    = column[Option[JsValue]]("EXTRA_DESCRIPTION_ATTRIBUTES")

    // scalastyle:off method.name
    def * =
      (
        typeId,
        noName,
        enName,
        collections,
        descAttrType,
        descAttrs
      )

    // scalastyle:on method.name
  }

  private def parseCollectionUUIDCol(colStr: Option[String]): Seq[CollectionUUID] = {
    colStr.map { str =>
      str.stripPrefix(",").stripSuffix(",").split(",").toSeq.map { uuidStr =>
        CollectionUUID.unsafeFromString(uuidStr)
      }
    }.getOrElse(Seq.empty[CollectionUUID])
  }

  /**
   * Converts an ConservationTypeRow tuple to an instance of ConservationType.
   *
   * @param t the ConservationTypeRow tuple to convert
   * @return the corresponding ConservationType
   */
  protected[dao] def fromConservationTypeRow(t: ConservationTypeRow): ConservationType = {
    ConservationType(
      id = t._1,
      noName = t._2,
      enName = t._3,
      collections = parseCollectionUUIDCol(t._4),
      extraDescriptionType = t._5,
      extraDescriptionAttributes =
        t._6.flatMap(a => Json.fromJson[Map[String, String]](a).asOpt)
    )
  }

}