package repositories.analysis.dao

import models.analysis.LeftoverSamples.LeftoverSample
import models.analysis.SampleStatuses.SampleStatus
import models.analysis.events._
import models.analysis._
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models._
import org.joda.time.DateTime
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.json.{JsValue, Json}
import repositories.shared.dao.ColumnTypeMappers
import slick.jdbc.{GetResult, JdbcProfile}

/**
 * Defines the table definitions needed to work with analysis data.
 * Also includes a few helpful functions to convert between rows and types.
 */
trait AnalysisTables
    extends HasDatabaseConfigProvider[JdbcProfile]
    with ColumnTypeMappers {

  import profile.api._

  val analysisTypeTable     = TableQuery[AnalysisTypeTable]
  val sampleObjTable        = TableQuery[SampleObjectTable]
  val treatmentTable        = TableQuery[TreatmentTable]
  val storageMediumTable    = TableQuery[StorageMediumTable]
  val storageContainerTable = TableQuery[StorageContainerTable]
  val sampleTypeTable       = TableQuery[SampleTypeTable]

  // scalastyle:off line.size.limit
  type AnalysisTypeRow =
    (
        AnalysisTypeId,
        Category,
        String,
        String,
        Option[String],
        Option[String],
        Option[String],
        Option[JsValue],
        Option[String],
        Option[JsValue]
    )

  type SampleObjectRow = (
      ObjectUUID,
      (Option[ObjectUUID], Option[ObjectType]),
      Boolean,
      MuseumId,
      SampleStatus,
      Option[ActorId],
      Option[ActorId],
      Option[DateTime],
      Option[String],
      Option[Int],
      (Option[String], Option[String]),
      SampleTypeId,
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

  type TreatmentRow        = (Int, String, String)
  type StorageMediumRow    = (Int, String, String)
  type StorageContainerRow = (Int, String, String)
  type SampleTypeRow       = (SampleTypeId, String, String, Option[String], Option[String])

  // scalastyle:on line.size.limit

  /**
   * Representation of the MUSARK_ANALYSIS.EVENT_TYPE table
   */
  class AnalysisTypeTable(
      val tag: Tag
  ) extends Table[AnalysisTypeRow](tag, Some(SchemaName), AnalysisEventTypeTableName) {

    val typeId       = column[AnalysisTypeId]("TYPE_ID", O.PrimaryKey, O.AutoInc)
    val category     = column[Category]("CATEGORY")
    val noName       = column[String]("NO_NAME")
    val enName       = column[String]("EN_NAME")
    val shortName    = column[Option[String]]("SHORT_NAME")
    val collections  = column[Option[String]]("COLLECTIONS")
    val descAttrType = column[Option[String]]("EXTRA_DESCRIPTION_TYPE")
    val descAttrs    = column[Option[JsValue]]("EXTRA_DESCRIPTION_ATTRIBUTES")
    val resAttrType  = column[Option[String]]("EXTRA_RESULT_TYPE")
    val resAttrs     = column[Option[JsValue]]("EXTRA_RESULT_ATTRIBUTES")

    // scalastyle:off method.name
    def * =
      (
        typeId,
        category,
        noName,
        enName,
        shortName,
        collections,
        descAttrType,
        descAttrs,
        resAttrType,
        resAttrs
      )

    // scalastyle:on method.name
  }

  class SampleObjectTable(
      val tag: Tag
  ) extends Table[SampleObjectRow](tag, Some(SchemaName), SampleObjectTableName) {

    val id               = column[ObjectUUID]("SAMPLE_UUID", O.PrimaryKey)
    val parentId         = column[Option[ObjectUUID]]("PARENT_OBJECT_UUID")
    val parentObjectType = column[Option[ObjectType]]("PARENT_OBJECT_TYPE")
    val isExtracted      = column[Boolean]("IS_EXTRACTED")
    val museumId         = column[MuseumId]("MUSEUM_ID")
    val status           = column[SampleStatus]("STATUS")
    val responsible      = column[Option[ActorId]]("RESPONSIBLE_ACTOR")
    val doneBy           = column[Option[ActorId]]("DONE_BY")
    val doneDate         = column[Option[DateTime]]("DONE_DATE")
    val sampleId         = column[Option[String]]("SAMPLE_ID")
    val sampleNum        = column[Option[Int]]("SAMPLE_NUM", O.AutoInc, O.Unique)
    val externalId       = column[Option[String]]("EXTERNAL_ID")
    val externalIdSource = column[Option[String]]("EXTERNAL_ID_SOURCE")
    val sampleTypeId     = column[SampleTypeId]("SAMPLE_TYPE_ID")
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
        doneBy,
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
   * Converts an AnalysisTypeRow tuple to an instance of AnalysisType.
   *
   * @param t the AnalysisTypeRow tuple to convert
   * @return the corresponding AnalysisType
   */
  protected[dao] def fromAnalysisTypeRow(t: AnalysisTypeRow): AnalysisType = {
    AnalysisType(
      id = t._1,
      category = t._2,
      noName = t._3,
      enName = t._4,
      shortName = t._5,
      collections = parseCollectionUUIDCol(t._6),
      extraDescriptionType = t._7,
      extraDescriptionAttributes =
        t._8.flatMap(a => Json.fromJson[Map[String, String]](a).asOpt),
      extraResultType = t._9,
      extraResultAttributes =
        t._10.flatMap(a => Json.fromJson[Map[String, String]](a).asOpt)
    )
  }

  protected[dao] def asSampleObjectTuple(so: SampleObject): SampleObjectRow = {
    (
      so.objectId.getOrElse(ObjectUUID.generate()),
      (so.parentObject.objectId, Some(so.parentObject.objectType)),
      so.isExtracted,
      so.museumId,
      so.status,
      so.responsible,
      so.doneByStamp.map(_.user),
      so.doneByStamp.map(_.date),
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
    val external     = tuple._11
    val sampleTypeId = tuple._12
    val size         = tuple._13
    val userStamps   = tuple._21

    SampleObject(
      objectId = Option(tuple._1),
      parentObject = (ParentObject.toParentObject _).tupled(tuple._2),
      isExtracted = tuple._3,
      museumId = tuple._4,
      status = tuple._5,
      responsible = tuple._6,
      doneByStamp = for {
        actorId  <- tuple._7
        doneDate <- tuple._8
      } yield ActorStamp(actorId, doneDate),
      sampleId = tuple._9,
      sampleNum = tuple._10,
      externalId = external._1.map(ExternalId(_, external._2)),
      sampleTypeId = sampleTypeId,
      size = for {
        value <- size._1
        unit  <- size._2
      } yield Size(unit, value),
      container = tuple._14,
      storageMedium = tuple._15,
      note = tuple._16,
      treatment = tuple._17,
      leftoverSample = tuple._18,
      description = tuple._19,
      originatedObjectUuid = tuple._20,
      registeredStamp = for {
        actor    <- userStamps._1
        dateTime <- userStamps._2
      } yield ActorStamp(actor, dateTime),
      updatedStamp = for {
        actor    <- userStamps._3
        dateTime <- userStamps._4
      } yield ActorStamp(actor, dateTime),
      isDeleted = tuple._22
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

  implicit val getEnrichedSampleResult = GetResult { res =>
    EnrichedSampleObject(
      museumNo = MuseumNo(res.nextString()),
      subNo = res.nextStringOption().map(SubNo.apply),
      term = res.nextString(),
      sampleObject = SampleObject(
        objectId = res.nextStringOption().map(ObjectUUID.unsafeFromString),
        originatedObjectUuid = ObjectUUID.unsafeFromString(res.nextString),
        parentObject = ParentObject(
          objectId = res.nextStringOption().map(ObjectUUID.unsafeFromString),
          objectType = ObjectType.unsafeFromString(res.nextString())
        ),
        isExtracted = res.nextBoolean(),
        museumId = MuseumId.fromInt(res.nextInt()),
        status = SampleStatus.unsafeFromInt(res.nextInt()),
        responsible = res.nextStringOption().map(ActorId.unsafeFromString),
        doneByStamp = {
          val maybeBy   = res.nextStringOption().map(ActorId.unsafeFromString)
          val maybeDate = res.nextTimestampOption().map(ts => new DateTime(ts.getTime))
          maybeBy.flatMap(by => maybeDate.map(ts => ActorStamp(by, ts)))
        },
        sampleId = res.nextStringOption(),
        sampleNum = res.nextIntOption(),
        externalId = {
          val maybeExtId  = res.nextStringOption()
          val maybeExtSrc = res.nextStringOption()
          maybeExtId.map(id => ExternalId(id, maybeExtSrc))
        },
        sampleTypeId = SampleTypeId(res.nextLong()),
        size = {
          val sz = res.nextDoubleOption()
          val un = res.nextStringOption()
          sz.flatMap(s => un.map(u => Size(u, s)))
        },
        container = res.nextStringOption(),
        storageMedium = res.nextStringOption(),
        treatment = res.nextStringOption(),
        leftoverSample = LeftoverSample.unsafeFromInt(res.nextInt()),
        description = res.nextStringOption(),
        note = res.nextStringOption(),
        registeredStamp = {
          val by  = ActorId.unsafeFromString(res.nextString())
          val dte = new DateTime(res.nextTimestamp().getTime)
          Some(ActorStamp(by, dte))
        },
        updatedStamp = {
          val by  = res.nextStringOption().map(ActorId.unsafeFromString)
          val dte = res.nextTimestampOption().map(ts => new DateTime(ts.getTime))
          by.flatMap(b => dte.map(d => ActorStamp(b, d)))
        },
        isDeleted = res.nextBoolean()
      )
    )
  }

}
