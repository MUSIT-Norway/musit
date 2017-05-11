package repositories.analysis.dao

import models.analysis.AnalysisStatuses.AnalysisStatus
import models.analysis.LeftoverSamples.LeftoverSample
import models.analysis._
import models.analysis.SampleStatuses.SampleStatus
import models.analysis.events.AnalysisResults.AnalysisResult
import models.analysis.events._
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models._
import org.joda.time.DateTime
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.json.{JsValue, Json}
import repositories.shared.dao.ColumnTypeMappers
import slick.jdbc.JdbcProfile

/**
 * Defines the table definitions needed to work with analysis data.
 * Also includes a few helpful functions to convert between rows and types.
 */
trait AnalysisTables
    extends HasDatabaseConfigProvider[JdbcProfile]
    with ColumnTypeMappers {

  import profile.api._

  val analysisTypeTable     = TableQuery[AnalysisTypeTable]
  val analysisTable         = TableQuery[AnalysisTable]
  val resultTable           = TableQuery[AnalysisResultTable]
  val sampleObjTable        = TableQuery[SampleObjectTable]
  val treatmentTable        = TableQuery[TreatmentTable]
  val storageMediumTable    = TableQuery[StorageMediumTable]
  val storageContainerTable = TableQuery[StorageContainerTable]
  val sampleTypeTable       = TableQuery[SampleTypeTable]

  // scalastyle:off line.size.limit
  type EventTypeRow =
    (AnalysisTypeId, Category, String, Option[String], Option[String], Option[JsValue])

  type EventRow = (
      Option[EventId],
      AnalysisTypeId,
      Option[ActorByIdOrName],
      Option[DateTime],
      Option[ActorId],
      Option[DateTime],
      Option[EventId],
      Option[ObjectUUID],
      Option[String],
      Option[AnalysisStatus],
      Option[CaseNumbers],
      JsValue
  )
  type ResultRow =
    (EventId, Option[ActorId], Option[DateTime], JsValue)

  type SampleObjectRow = (
      ObjectUUID,
      (Option[ObjectUUID], ObjectType),
      Boolean,
      MuseumId,
      SampleStatus,
      Option[ActorByIdOrName],
      Option[DateTime],
      Option[String],
      Option[Int],
      (Option[String], Option[String]),
      Option[SampleTypeId],
      (Option[Double], Option[String]),
      Option[String],
      Option[String],
      Option[String],
      Option[String],
      LeftoverSample,
      Option[String],
      ObjectUUID,
      (Option[ActorId], Option[DateTime], Option[ActorId], Option[DateTime]),
      Boolean
  )

  type TreatmentRow = (Int, String, String)

  type StorageMediumRow    = (Int, String, String)
  type StorageContainerRow = (Int, String, String)

  type SampleTypeRow = (SampleTypeId, String, String, Option[String], Option[String])

  // scalastyle:on line.size.limit

  /**
   * Representation of the MUSARK_ANALYSIS.EVENT_TYPE table
   */
  class AnalysisTypeTable(
      val tag: Tag
  ) extends Table[EventTypeRow](tag, Some(SchemaName), AnalysisEventTypeTableName) {

    val typeId      = column[AnalysisTypeId]("TYPE_ID", O.PrimaryKey)
    val category    = column[Category]("CATEGORY")
    val name        = column[String]("NAME")
    val shortName   = column[Option[String]]("SHORT_NAME")
    val collections = column[Option[String]]("COLLECTIONS")
    // TODO: Need col to tag if used by Zoology, Botanics, Archeology or Ethnography?
    val attributes = column[Option[JsValue]]("ATTRIBUTES")

    // scalastyle:off method.name
    def * = (typeId, category, name, shortName, collections, attributes)

    // scalastyle:on method.name
  }

  /**
   * Representation of the MUSARK_ANALYSIS.EVENT table
   */
  class AnalysisTable(
      val tag: Tag
  ) extends Table[EventRow](tag, Some(SchemaName), AnalysisEventTableName) {

    val id             = column[EventId]("EVENT_ID", O.PrimaryKey, O.AutoInc)
    val typeId         = column[AnalysisTypeId]("TYPE_ID")
    val doneBy         = column[Option[ActorByIdOrName]]("DONE_BY")
    val doneDate       = column[Option[DateTime]]("DONE_DATE")
    val registeredBy   = column[Option[ActorId]]("REGISTERED_BY")
    val registeredDate = column[Option[DateTime]]("REGISTERED_DATE")
    val partOf         = column[Option[EventId]]("PART_OF")
    val objectUuid     = column[Option[ObjectUUID]]("OBJECT_UUID")
    val note           = column[Option[String]]("NOTE")
    val caseNumbers    = column[Option[CaseNumbers]]("CASE_NUMBERS")
    val status         = column[Option[AnalysisStatus]]("STATUS")
    val eventJson      = column[JsValue]("EVENT_JSON")

    // scalastyle:off method.name line.size.limit
    def * =
      (
        id.?,
        typeId,
        doneBy,
        doneDate,
        registeredBy,
        registeredDate,
        partOf,
        objectUuid,
        note,
        status,
        caseNumbers,
        eventJson
      )

    // scalastyle:on method.name line.size.limit
  }

  /**
   * Representation of the MUSARK_ANALUSIS.RESULT table
   */
  class AnalysisResultTable(
      val tag: Tag
  ) extends Table[ResultRow](tag, Some(SchemaName), AnalysisResultTableName) {

    val eventId        = column[EventId]("EVENT_ID", O.PrimaryKey)
    val registeredBy   = column[Option[ActorId]]("REGISTERED_BY")
    val registeredDate = column[Option[DateTime]]("REGISTERED_DATE")
    val resultJson     = column[JsValue]("RESULT_JSON")

    // scalastyle:off method.name
    def * = (eventId, registeredBy, registeredDate, resultJson)

    // scalastyle:on method.name
  }

  class SampleObjectTable(
      val tag: Tag
  ) extends Table[SampleObjectRow](tag, Some(SchemaName), SampleObjectTableName) {

    val id               = column[ObjectUUID]("SAMPLE_UUID", O.PrimaryKey)
    val parentId         = column[Option[ObjectUUID]]("PARENT_OBJECT_UUID")
    val parentObjectType = column[ObjectType]("PARENT_OBJECT_TYPE")
    val isExtracted      = column[Boolean]("IS_EXTRACTED")
    val museumId         = column[MuseumId]("MUSEUM_ID")
    val status           = column[SampleStatus]("STATUS")
    val responsible      = column[Option[ActorByIdOrName]]("RESPONSIBLE_ACTOR")
    val doneDate         = column[Option[DateTime]]("DONE_DATE")
    val sampleId         = column[Option[String]]("SAMPLE_ID")
    val sampleNum        = column[Option[Int]]("SAMPLE_NUM", O.AutoInc, O.Unique)
    val externalId       = column[Option[String]]("EXTERNAL_ID")
    val externalIdSource = column[Option[String]]("EXTERNAL_ID_SOURCE")
    val sampleTypeId     = column[Option[SampleTypeId]]("SAMPLE_TYPE_ID")
    val size             = column[Option[Double]]("SAMPLE_SIZE")
    val sizeUnit         = column[Option[String]]("SAMPLE_SIZE_UNIT")
    val container        = column[Option[String]]("SAMPLE_CONTAINER")
    val storageMedium    = column[Option[String]]("STORAGE_MEDIUM")
    val treatment        = column[Option[String]]("TREATMENT")
    val leftoverSample   = column[LeftoverSample]("LEFTOVER_SAMPLE")
    val description      = column[Option[String]]("DESCRIPTION")
    val note             = column[Option[String]]("NOTE")
    val originatedFrom   = column[ObjectUUID]("ORIGINATED_OBJECT_UUID")
    val registeredBy     = column[Option[ActorId]]("REGISTERED_BY")
    val registeredDate   = column[Option[DateTime]]("REGISTERED_DATE")
    val updatedBy        = column[Option[ActorId]]("UPDATED_BY")
    val updatedDate      = column[Option[DateTime]]("UPDATED_DATE")
    val isDeleted        = column[Boolean]("IS_DELETED")

    // scalastyle:off method.name line.size.limit
    def * =
      (
        id,
        (parentId, parentObjectType),
        isExtracted,
        museumId,
        status,
        responsible,
        doneDate,
        sampleId,
        sampleNum,
        (externalId, externalIdSource),
        sampleTypeId,
        (size, sizeUnit),
        container,
        storageMedium,
        note,
        treatment,
        leftoverSample,
        description,
        originatedFrom,
        (registeredBy, registeredDate, updatedBy, updatedDate),
        isDeleted
      )

    // scalastyle:off method.name line.size.limit

  }

  /**
   * Representation of the MUSARK_ANALYSIS.SAMPLE_TYPE table
   */
  class SampleTypeTable(val tag: Tag)
      extends Table[SampleTypeRow](tag, Some(SchemaName), SampleTypeTableName) {
    val sampleTypeId    = column[SampleTypeId]("SAMPLETYPE_ID")
    val noSampleType    = column[String]("NO_SAMPLETYPE")
    val enSampleType    = column[String]("EN_SAMPLETYPE")
    val noSampleSubType = column[Option[String]]("NO_SAMPLESUBTYPE")
    val enSampleSubType = column[Option[String]]("EN_SAMPLESUBTYPE")

    // scalastyle:off method.name
    def * = (sampleTypeId, noSampleType, enSampleType, noSampleSubType, enSampleSubType)

    // scalastyle:on method.name
  }

  /**
   * Representation of the MUSARK_ANALYSIS.TREATMENT table
   */
  class TreatmentTable(val tag: Tag)
      extends Table[TreatmentRow](tag, Some(SchemaName), TreatmentTableName) {
    val treatmentId = column[Int]("TREATMENT_ID")
    val noTreatment = column[String]("NO_TREATMENT")
    val enTreatment = column[String]("EN_TREATMENT")

    // scalastyle:off method.name
    def * = (treatmentId, noTreatment, enTreatment)

    // scalastyle:on method.name
  }

  /**
   * Representation of the MUSARK_ANALYSIS.StorageContainer table
   */
  class StorageContainerTable(val tag: Tag)
      extends Table[StorageContainerRow](
        tag,
        Some(SchemaName),
        StorageContainerTableName
      ) {
    val storageContainerId = column[Int]("STORAGECONTAINER_ID")
    val noStorageContainer = column[String]("NO_STORAGECONTAINER")
    val enStorageContainer = column[String]("EN_STORAGECONTAINER")

    // scalastyle:off method.name
    def * = (storageContainerId, noStorageContainer, enStorageContainer)

    // scalastyle:on method.name
  }

  /**
   * Representation of the MUSARK_ANALYSIS.STORAGEMEDIUM table
   */
  class StorageMediumTable(val tag: Tag)
      extends Table[StorageMediumRow](tag, Some(SchemaName), StorageMediumTableName) {
    val storageMediumId = column[Int]("STORAGEMEDIUM_ID")
    val noStorageMedium = column[String]("NO_STORAGEMEDIUM")
    val enStorageMedium = column[String]("EN_STORAGEMEDIUM")

    // scalastyle:off method.name
    def * = (storageMediumId, noStorageMedium, enStorageMedium)

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
   * Converts an EventTypeRow tuple to an instance of AnalysisType.
   *
   * @param t the EventTypeRow tuple to convert
   * @return the corresponding AnalysisType
   */
  protected[dao] def fromAnalysisTypeRow(t: EventTypeRow): AnalysisType = {
    AnalysisType(
      id = t._1,
      category = t._2,
      name = t._3,
      shortName = t._4,
      collections = parseCollectionUUIDCol(t._5),
      extraAttributes = t._6.flatMap(a => Json.fromJson[Map[String, String]](a).asOpt)
    )
  }

  /**
   * Converts an AnalysisEvent into an EventRow tuple that can be inserted into
   * the database table.
   *
   * @param event the event to convert to a tuple
   * @return an EventRow tuple
   */
  protected[dao] def asEventTuple(event: AnalysisEvent): EventRow = {
    (
      event.id,
      event.analysisTypeId,
      event.doneBy,
      event.doneDate,
      event.registeredBy,
      event.registeredDate,
      event.partOf,
      event.objectId,
      event.note,
      event.status,
      event.caseNumbers,
      Json.toJson[AnalysisEvent](event)
    )
  }

  /**
   * Converts an EventRow tuple into an instance of AnalysisEvent.
   *
   * @param tuple EventRow
   * @return an Option of AnalysisEvent.
   */
  protected[dao] def toAnalysisEvent(tuple: EventRow): Option[AnalysisEvent] =
    Json.fromJson[AnalysisEvent](tuple._12).asOpt.map {
      case a: Analysis            => a.copy(id = tuple._1)
      case ac: AnalysisCollection => ac.copy(id = tuple._1)
      case sc: SampleCreated      => sc.copy(id = tuple._1)
    }

  protected[dao] def toAnalysis(tuple: EventRow): Option[Analysis] =
    Json.fromJson[AnalysisEvent](tuple._12).asOpt.flatMap {
      case a: Analysis => Some(a.copy(id = tuple._1))
      case _           => None
    }

  protected[dao] def toAnalysisCollection(tuple: EventRow): Option[AnalysisCollection] =
    Json.fromJson[AnalysisEvent](tuple._12).asOpt.flatMap {
      case ac: AnalysisCollection => Some(ac.copy(id = tuple._1))
      case _                      => None
    }

  /**
   * Converts an AnalysisResult into a ResultRow tuple.
   *
   * @param eid the EventId the AnalysisResult belongs to
   * @param res the AnalysisResult to convert to a tuple
   * @return the corresponding ResultRow tuple
   */
  protected[dao] def asResultTuple(eid: EventId, res: AnalysisResult): ResultRow = {
    (
      eid,
      res.registeredBy,
      res.registeredDate,
      Json.toJson[AnalysisResult](res)
    )
  }

  /**
   * Converts a ResultRow tuple into an instance of an AnalysisResult
   *
   * @param tuple the ResultRow tuple to convert
   * @return an Option of the corresponding AnalysisResult
   */
  protected[dao] def fromResultRow(tuple: ResultRow): Option[AnalysisResult] =
    Json.fromJson[AnalysisResult](tuple._4).asOpt

  protected[dao] def fromResultRow(
      maybeTuple: Option[ResultRow]
  ): Option[AnalysisResult] =
    maybeTuple.flatMap(fromResultRow)
  protected[dao] def asSampleObjectTuple(so: SampleObject): SampleObjectRow = {
    (
      so.objectId.getOrElse(ObjectUUID.generate()),
      (so.parentObjectId, so.parentObjectType),
      so.isExtracted,
      so.museumId,
      so.status,
      so.responsible,
      so.doneDate,
      so.sampleId,
      so.sampleNum,
      (so.externalId.map(_.value), so.externalId.flatMap(_.source)),
      so.sampleTypeId,
      (so.size.map(_.value), so.size.map(_.unit)),
      so.container,
      so.storageMedium,
      so.note,
      so.treatment,
      so.leftoverSample,
      so.description,
      so.originatedObjectUuid,
      (
        so.registeredStamp.map(_.user),
        so.registeredStamp.map(_.date),
        so.updatedStamp.map(_.user),
        so.updatedStamp.map(_.date)
      ),
      so.isDeleted
    )
  }

  /**
   * Converts a SampleObjectRow tuple into an instance of SampleObject
   *
   * @param tuple the SampleObjectRow to convert
   * @return an instance of SampleObject
   */
  protected[dao] def fromSampleObjectRow(tuple: SampleObjectRow): SampleObject = {
    val parentObject = tuple._2
    val external     = tuple._10
    val sampleTypeId = tuple._11
    val size         = tuple._12
    val userStamps   = tuple._20

    SampleObject(
      objectId = Option(tuple._1),
      parentObjectId = parentObject._1,
      parentObjectType = parentObject._2,
      isExtracted = tuple._3,
      museumId = tuple._4,
      status = tuple._5,
      responsible = tuple._6,
      doneDate = tuple._7,
      sampleId = tuple._8,
      sampleNum = tuple._9,
      externalId = external._1.map(ExternalId(_, external._2)),
      sampleTypeId = sampleTypeId,
      size = for {
        value <- size._1
        unit  <- size._2
      } yield Size(unit, value),
      container = tuple._13,
      storageMedium = tuple._14,
      note = tuple._15,
      treatment = tuple._16,
      leftoverSample = tuple._17,
      description = tuple._18,
      originatedObjectUuid = tuple._19,
      registeredStamp = for {
        actor    <- userStamps._1
        dateTime <- userStamps._2
      } yield ActorStamp(actor, dateTime),
      updatedStamp = for {
        actor    <- userStamps._3
        dateTime <- userStamps._4
      } yield ActorStamp(actor, dateTime),
      isDeleted = tuple._21
    )
  }

  /**
   * Converts a TreatmentRow tuple into an instance of Treatment
   *
   * @param tuple the TreatmentRow to convert
   * @return an instance of Treatment
   */
  protected[dao] def fromTreatmentRow(tuple: TreatmentRow): Treatment =
    Treatment(treatmentId = tuple._1, noTreatment = tuple._2, enTreatment = tuple._3)

  /**
   * Converts a StorageContainerRow tuple into an instance of StorageContainer
   *
   * @param tuple the StorageContainerRow to convert
   * @return an instance of StorageContainer
   */
  protected[dao] def fromStorageContainerRow(
      tuple: StorageContainerRow
  ): StorageContainer =
    StorageContainer(
      storageContainerId = tuple._1,
      noStorageContainer = tuple._2,
      enStorageContainer = tuple._3
    )

  /**
   * Converts a StorageMediumRow tuple into an instance of StorageMedium
   *
   * @param tuple the StorageMediumRow to convert
   * @return an instance of StorageMedium
   */
  protected[dao] def fromStorageMediumRow(tuple: StorageMediumRow): StorageMedium =
    StorageMedium(
      storageMediumId = tuple._1,
      noStorageMedium = tuple._2,
      enStorageMedium = tuple._3
    )

  /**
   * Converts a SampleTypeRow tuple into an instance of SampleType
   *
   * @param tuple the SampleTypeRow to convert
   * @return an instance of SampleType
   */
  protected[dao] def fromSampleTypeRow(
      tuple: SampleTypeRow
  ): SampleType =
    SampleType(
      sampleTypeId = tuple._1,
      noSampleType = tuple._2,
      enSampleType = tuple._3,
      noSampleSubType = tuple._4,
      enSampleSubType = tuple._5
    )

}
