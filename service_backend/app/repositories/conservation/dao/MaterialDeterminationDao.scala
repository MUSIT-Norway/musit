package repositories.conservation.dao

import com.google.inject.{Inject, Singleton}
import models.conservation.{MaterialEthnography, _}
import models.conservation.events._
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models.EventId
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import repositories.conservation.DaoUtils

import scala.concurrent.ExecutionContext
@Singleton
class MaterialDeterminationDao @Inject()(
    implicit
    override val dbConfigProvider: DatabaseConfigProvider,
    override val ec: ExecutionContext,
    override val objectEventDao: ObjectEventDao,
    override val daoUtils: DaoUtils,
    override val actorRoleDao: ActorRoleDateDao,
    override val eventDocumentDao: EventDocumentDao
) extends ConservationEventDao[MaterialDetermination]
    with ConservationEventTableProvider {

  override val logger = Logger(classOf[MaterialDeterminationDao])

  import profile.api._

  private val materialArchTable    = TableQuery[MaterialArchaeologyTable]
  private val materialEthnTable    = TableQuery[MaterialEthnoTable]
  private val materialNumisTable   = TableQuery[MaterialNumisTable]
  private val eaEventMaterialTable = TableQuery[EaEventMaterialTable]

  override def removeSpecialEventAttributes(
      event: ConservationEvent
  ): ConservationEvent = {
    val me = event.asInstanceOf[MaterialDetermination]
    me.withOutSpesialMatrAndSorting
  }

  def insertEaEventMaterialAction(
      materialInfo: MaterialInfo,
      eventId: EventId
  ): DBIO[Int] = {
    val action = eaEventMaterialTable += EaEventMaterial(eventId, materialInfo)
    action
  }

  override def insertSpecialAttributes(
      eventId: EventId,
      event: ConservationEvent
  ): DBIO[Unit] = {
    val smss = event.asInstanceOf[MaterialDetermination].materialInfo
    smss match {
      case None => DBIO.successful(())
      case Some(seqSms) => {
        val actions = seqSms.map(sms => insertEaEventMaterialAction(sms, eventId))
        DBIO.sequence(actions).map(_ => ())
      }
    }
  }

  private def deleteSpecialAttributesAction(eventId: EventId): DBIO[Int] = {
    val q      = eaEventMaterialTable.filter(oe => oe.eventId === eventId)
    val action = q.delete
    action
  }

  override def updateSpecialAttributes(
      eventId: EventId,
      event: ConservationEvent
  ): DBIO[Unit] = {
    for {
      deleted  <- deleteSpecialAttributesAction(eventId)
      inserted <- insertSpecialAttributes(eventId, event)
    } yield inserted
  }

  private def getSpecialAttributes(
      eventId: EventId
  ): FutureMusitResult[Seq[MaterialInfo]] = {
    val action = {
      eaEventMaterialTable
        .filter(oe => oe.eventId === eventId)
        .map(sms => (sms.materialId, sms.materialExtra, sms.sorting))
        .result
        .map(_.map {
          case (materialId, materialExtra, sorting) =>
            MaterialInfo(materialId, materialExtra, sorting)
        })
    }
    val res = action
    daoUtils.dbRun(
      res,
      s"An unexpected error occurred fetching objects in getSpecialAttributes for event $eventId"
    )
  }

  override def enrichWithSpecialAttributes(
      eventId: EventId,
      event: ConservationEvent
  ): FutureMusitResult[ConservationEvent] = {
    val md = event.asInstanceOf[MaterialDetermination]
    getSpecialAttributes(eventId).map(m => md.copy(materialInfo = Some(m)))
  }

  def getArchaeologyMaterialList: FutureMusitResult[Seq[MaterialArchaeology]] = {
    val action =
      materialArchTable.filter(mat => mat.hidden === 0).result
    daoUtils.dbRun(action, "getArchaeologyMaterialList failed")
  }

  def getEthnographyMaterialList: FutureMusitResult[Seq[MaterialEthnography]] = {
    val action =
      materialEthnTable.filter(mat => mat.hidden === 0).result
    daoUtils.dbRun(action, "getEthnographyMaterialList failed")
  }

  def getNumismaticMaterialList: FutureMusitResult[Seq[MaterialNumismatic]] = {
    val action =
      materialNumisTable.filter(mat => mat.hidden === 0).result
    daoUtils.dbRun(action, "getNumismaticMaterialList failed")
  }

  def getArchaeologyMaterial(materialId: Int): FutureMusitResult[MaterialArchaeology] = {
    val action =
      materialArchTable.filter(mat => mat.materialId === materialId).result.head
    daoUtils.dbRun(action, "getArchaeologyMaterial failed")
  }

  def getEthnographyMaterial(materialId: Int): FutureMusitResult[MaterialEthnography] = {
    val action =
      materialEthnTable.filter(mat => mat.materialId === materialId).result.head
    daoUtils.dbRun(action, "getEthnographyMaterial failed")
  }

  def getNumismaticMaterial(materialId: Int): FutureMusitResult[MaterialNumismatic] = {
    val action =
      materialNumisTable.filter(mat => mat.materialId === materialId).result.head
    daoUtils.dbRun(action, "getNumismaticMaterial failed")
  }

  private class MaterialArchaeologyTable(tag: Tag)
      extends Table[MaterialArchaeology](
        tag,
        Some(SchemaName),
        MaterialArhaeoTableName
      ) {
    val materialId = column[Int]("MATERIAL_ID")
    val noMaterial = column[String]("NO_MATERIAL")
    val enMaterial = column[Option[String]]("EN_MATERIAL")
    val hidden     = column[Int]("HIDDEN")

    // scalastyle:off method.name
    def * =
      (materialId, noMaterial, enMaterial) <> ((MaterialArchaeology.apply _).tupled, MaterialArchaeology.unapply)

    // scalastyle:on method.name

  }

  private class MaterialEthnoTable(tag: Tag)
      extends Table[MaterialEthnography](
        tag,
        Some(SchemaName),
        MaterialEthnoTableName
      ) {
    val materialId        = column[Int]("MATERIAL_ID")
    val noMaterial        = column[String]("NO_MATERIAL")
    val noMaterialType    = column[Option[String]]("NO_MATERIAL_TYPE")
    val noMaterialElement = column[Option[String]]("NO_MATERIAL_ELEMENT")
    val enMaterial        = column[Option[String]]("EN_MATERIAL")
    val enMaterialType    = column[Option[String]]("EN_MATERIAL_TYPE")
    val enMaterialElement = column[Option[String]]("EN_MATERIAL_ELEMENT")
    val frMaterial        = column[Option[String]]("FR_MATERIAL")
    val frMaterialType    = column[Option[String]]("FR_MATERIAL_TYPE")
    val frMaterialElement = column[Option[String]]("FR_MATERIAL_ELEMENT")
    val hidden            = column[Int]("HIDDEN")

    // scalastyle:off method.name
    def * =
      (
        materialId,
        noMaterial,
        noMaterialType,
        noMaterialElement,
        enMaterial,
        enMaterialType,
        enMaterialElement,
        frMaterial,
        frMaterialType,
        frMaterialElement
      ) <> ((MaterialEthnography.apply _).tupled, MaterialEthnography.unapply)

    // scalastyle:on method.name

  }

  private class MaterialNumisTable(tag: Tag)
      extends Table[MaterialNumismatic](
        tag,
        Some(SchemaName),
        MaterialNumisTableName
      ) {
    val materialId = column[Int]("MATERIAL_ID")
    val noMaterial = column[String]("NO_MATERIAL")
    val enMaterial = column[Option[String]]("EN_MATERIAL")
    val hidden     = column[Int]("HIDDEN")

    // scalastyle:off method.name
    def * =
      (materialId, noMaterial, enMaterial) <> ((MaterialNumismatic.apply _).tupled, MaterialNumismatic.unapply)

    // scalastyle:on method.name

  }

  override def insertAction(event: EventRow): DBIO[EventId] = {
    super.insertAction(event)
    eventTable returning eventTable.map(_.eventId) += event
  }

  private class EaEventMaterialTable(tag: Tag)
      extends Table[EaEventMaterial](
        tag,
        Some(SchemaName),
        EaEventMaterialTableName
      ) {
    val eventId       = column[EventId]("EVENT_ID")
    val materialId    = column[Int]("MATERIAL_ID")
    val materialExtra = column[Option[String]]("MATERIAL_EXTRA")
    val sorting       = column[Option[Int]]("SORTING")

    val create = (
        eventId: EventId,
        materialId: Int,
        materialExtra: Option[String],
        sorting: Option[Int]
    ) =>
      EaEventMaterial(
        eventId = eventId,
        materialInfo = MaterialInfo(materialId, materialExtra, sorting)
    )

    val destroy = (eem: EaEventMaterial) =>
      Some(
        (
          eem.eventId,
          eem.materialInfo.materialId,
          eem.materialInfo.materialExtra,
          eem.materialInfo.sorting
        )
    )

    // scalastyle:off method.name
    def * =
      (eventId, materialId, materialExtra, sorting) <> (create.tupled, destroy)

    // scalastyle:on method.name

  }

}
