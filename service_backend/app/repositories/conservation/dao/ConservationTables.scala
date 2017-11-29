package repositories.conservation.dao

import models.conservation.{ConditionCode, TreatmentKeyword, TreatmentMaterial}
import models.conservation.events.ConservationType
import no.uio.musit.models.{CollectionUUID, EventTypeId}
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

  val conservationTypeTable  = TableQuery[ConservationTypeTable]
  val treatmentMaterialTable = TableQuery[TreatmentMaterialTable]
  val treatmentKeywordTable  = TableQuery[TreatmentKeywordTable]
  val conditionCode          = TableQuery[ConditionCodeTable]

  // scalastyle:off line.size.limit
  type ConservationTypeRow =
    (
        EventTypeId,
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

    val typeId       = column[EventTypeId]("TYPE_ID", O.PrimaryKey, O.AutoInc)
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

  class TreatmentMaterialTable(val tag: Tag)
      extends Table[TreatmentMaterial](
        tag,
        Some(SchemaName),
        TreatmentMaterialTableName
      ) {
    val id     = column[Int]("MATERIAL_ID")
    val noTerm = column[String]("NO_MATERIAL")
    val enTerm = column[String]("EN_MATERIAL")

    // scalastyle:off method.name
    def * =
      (id, noTerm, enTerm) <> ((TreatmentMaterial.apply _).tupled, TreatmentMaterial.unapply)

    // scalastyle:on method.name
  }

  class TreatmentKeywordTable(val tag: Tag)
      extends Table[TreatmentKeyword](
        tag,
        Some(SchemaName),
        TreatmentKeywordTableName
      ) {
    val id     = column[Int]("KEYWORD_ID")
    val noTerm = column[String]("NO_KEYWORD")
    val enTerm = column[String]("EN_KEYWORD")

    // scalastyle:off method.name
    def * =
      (id, noTerm, enTerm) <> ((TreatmentKeyword.apply _).tupled, TreatmentKeyword.unapply)

    // scalastyle:on method.name
  }

  class ConditionCodeTable(val tag: Tag)
      extends Table[ConditionCode](
        tag,
        Some(SchemaName),
        ConditionCodeTableName
      ) {
    val conditionCode = column[Int]("CONDITION_CODE")
    val noCondition   = column[String]("NO_CONDITION")
    val enCondition   = column[String]("EN_CONDITION")

    // scalastyle:off method.name
    def * =
      (conditionCode, noCondition, enCondition) <> ((ConditionCode.apply _).tupled, ConditionCode.unapply)

    // scalastyle:on method.name
  }

}
