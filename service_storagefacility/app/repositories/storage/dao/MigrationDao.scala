package repositories.storage.dao

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.google.inject.{Inject, Singleton}
import migration.MigrationVerification
import models.storage.event.{EventTypeId, EventTypeRegistry}
import models.storage.event.EventTypeRegistry.ObsSubEvents.{
  ObsHumidityType,
  ObsHypoxicAirType,
  ObsPestType,
  ObsTemperatureType
}
import models.storage.event.EventTypeRegistry.TopLevelEvents._
import models.storage.event.EventTypeRegistry._
import models.storage.event.dto._
import no.uio.musit.models._
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.storage.old_dao.{EventTables => OldEventTables}
import slick.basic.DatabasePublisher

import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class MigrationDao @Inject()(
    implicit val dbConfigProvider: DatabaseConfigProvider,
    val as: ActorSystem,
    val mat: Materializer
) extends StorageTables
    with OldEventTables
    with EventTables {

  val logger = Logger(classOf[MigrationDao])

  import profile.api._

  // scalastyle:off
  type ObjectRow = (
      (
          Option[ObjectId],
          Option[ObjectUUID],
          MuseumId,
          String,
          Option[Long],
          Option[String],
          Option[Long],
          Option[Long],
          Boolean,
          String,
          Option[String],
          Option[Long],
          Option[Int]
      )
  )
  // scalastyle:on

  val objTable = TableQuery[ObjectTable]

  def getObjectUUIDsForObjectIds(
      ids: Seq[ObjectId]
  ): Future[Map[ObjectId, (ObjectUUID, MuseumId)]] = {
    val q = objTable.filter(_.id inSet ids).map(n => (n.id, n.uuid, n.museumId))
    db.run(q.result)
      .map(tuples => tuples.map(t => (t._1, (t._2.get, t._3))).toMap)
      .recover {
        case NonFatal(ex) =>
          logger.error("An error occurred fetching ObjectUUIDs", ex)
          throw ex
      }
  }

  // We can do this, since there are not that many nodes.
  def getAllNodeIds: Future[Map[StorageNodeDatabaseId, (StorageNodeId, MuseumId)]] = {
    val q = storageNodeTable.map(n => (n.id, n.uuid, n.museumId))
    db.run(q.result).map(tuples => tuples.map(t => (t._1, (t._2.get, t._3))).toMap)
  }

  def countAll(): Future[MigrationVerification] = {

    def countType(et: EventTypeId): DBIO[Int] = {
      storageEventTable.filter(_.eventTypeId === et).map(_.eventId).length.result
    }

    val query = for {
      a <- countType(ControlEventType.id)
      b <- countType(ObservationEventType.id)
      c <- countType(EnvRequirementEventType.id)
      d <- countType(MoveNodeType.id)
      e <- countType(MoveObjectType.id)
    } yield {
      MigrationVerification(a, b, c, d, e)
    }

    db.run(query)
  }

  def getEnvReqDetails(base: BaseEventDto): Future[ExtendedDto] = {
    db.run(envReqTable.filter(_.id === base.id).result.headOption)
      .map(er => ExtendedDto(base, er.get))
      .recover {
        case NonFatal(ex) =>
          logger.error("An error occurred when fetchin envreq details", ex)
          throw ex
      }
  }

  def enrichSubObs(subObs: Seq[BaseEventDto]): DBIO[Seq[EventDto]] = {
    DBIO.sequence {
      subObs.map { base =>
        ObsSubEvents.unsafeFromId(base.eventTypeId) match {
          case ObsHumidityType | ObsTemperatureType | ObsHypoxicAirType =>
            obsFromToTable
              .filter(_.id === base.id.get)
              .result
              .headOption
              .map(_.map(ft => ExtendedDto(base, ft)).getOrElse(base))

          case ObsPestType =>
            lifeCycleTable.filter(_.eventId === base.id.get).result.map {
              case Nil  => base
              case rows => ExtendedDto(base, ObservationPestDto(rows))
            }

          case others =>
            DBIO.successful(base)
        }
      }
    }
  }

  def getObsDetails(parent: BaseEventDto): Future[BaseEventDto] = {
    val q = for {
      subObs   <- eventBaseTable.filter(_.partOf === parent.id).result
      enriched <- enrichSubObs(subObs)
    } yield {
      parent.copy(
        relatedSubEvents = Seq(RelatedEvents(EventRelations.PartsOfRelation, enriched))
      )
    }

    db.run(q)
  }

  def getCtrlDetails(parent: BaseEventDto): Future[BaseEventDto] = {
    val q = for {
      subCtrls <- eventBaseTable.filter(_.partOf === parent.id).result
      subObs   <- eventBaseTable.filter(_.partOf inSet subCtrls.flatMap(t => t.id)).result
      enriched <- enrichSubObs(subObs)
    } yield {
      subCtrls.map { c =>
        val o = enriched.filter(_.partOf == c.id)
        c.copy(relatedSubEvents = Seq(RelatedEvents(EventRelations.PartsOfRelation, o)))
      }
    }

    db.run(q).map { subs =>
      parent.copy(
        relatedSubEvents = Seq(RelatedEvents(EventRelations.PartsOfRelation, subs))
      )
    }
  }

  def getAllBaseEvents: DatabasePublisher[BaseEventDto] = {
    val q = eventBaseTable
      .filter(_.eventTypeId inSet TopLevelEvents.values.map(_.id))
      .sortBy(_.id.asc)

    db.stream(q.result)
  }

  // scalastyle:off
  def enrichTopLevelEvent(dto: BaseEventDto): Future[BaseEventDto] = {
    val t = EventTypeRegistry.unsafeFromId(dto.eventTypeId)
    val query = for {
      actors <- eventActorsTable.filter(_.eventId === dto.id).result
      objects <- t match {
                  case MoveObjectType =>
                    eventObjectsTable.filter(_.eventId === dto.id).result

                  case _ =>
                    placesAsObjectsTable.filter(_.eventId === dto.id).result.map { rps =>
                      rps.map { rp =>
                        EventRoleObject(
                          eventId = rp.eventId,
                          roleId = rp.roleId,
                          objectId = ObjectId(rp.placeId.underlying),
                          eventTypeId = rp.eventTypeId
                        )
                      }
                    }
                }
      places <- eventPlacesTable.filter(_.eventId === dto.id).result
    } yield {
      dto.copy(
        relatedActors = actors,
        relatedObjects = objects,
        relatedPlaces = places
      )
    }
    db.run(query)
  }

  // scalastyle:on

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
      newCollectionId
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
    val newCollectionId  = column[Option[Int]]("NEW_COLLECTION_ID")

  }

}
