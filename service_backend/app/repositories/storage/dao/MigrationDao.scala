package repositories.storage.dao

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.google.inject.{Inject, Singleton}
import migration.MigrationVerification
import models.storage.event.EventTypeRegistry.ObsSubEvents.{
  ObsHumidityType,
  ObsHypoxicAirType,
  ObsPestType,
  ObsTemperatureType
}
import models.storage.event.EventTypeRegistry.TopLevelEvents._
import models.storage.event.EventTypeRegistry._
import models.storage.event.dto._
import models.storage.event.{EventTypeId, EventTypeRegistry}
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import no.uio.musit.models._
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.storage.dao.MigrationDao.{AllEventsRow, AllTupleType}
import repositories.storage.old_dao.{EventTables => OldEventTables}
import slick.basic.DatabasePublisher

import scala.concurrent.Future
import scala.util.control.NonFatal

// TODO: This can be removed when Migration has been performed.

object MigrationDao {
  type AllEventsRow =
    (BaseEventDto, Option[ObjectUUID], Option[MuseumId])

  import java.sql.{Timestamp => JSqlTimestamp}

  type AllTupleType = (
      // base
      Long,
      Int,
      JSqlTimestamp,
      Option[String],
      Option[Long],
      Option[Long],
      Option[String],
      Option[Double],
      Option[String],
      Option[JSqlTimestamp],
      // actors
      Option[Int],
      Option[String],
      // places
      Option[Int],
      Option[Long],
      // places as obj
      Option[Int],
      Option[Long],
      // objects
      Option[Int],
      Option[Long],
      // object uuid
      Option[String],
      // obj museumId
      Option[Int]
  )
}

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

  val objTable      = TableQuery[ObjectTable]
  val migratedTable = TableQuery[MigratedEventsTable]

  // We can do this, since there are not that many nodes.
  def getAllNodeIds: Future[Map[StorageNodeDatabaseId, (StorageNodeId, MuseumId)]] = {
    val q = storageNodeTable.map(n => (n.id, n.uuid, n.museumId))
    db.run(q.result).map(tuples => tuples.map(t => (t._1, (t._2, t._3))).toMap)
  }

  def countOld: Future[Int] = {
    val q = eventBaseTable
      .filter(_.eventTypeId inSet TopLevelEvents.values.map(_.id))
      .length
      .result

    db.run(q)
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
    } yield MigrationVerification(a, b, c, d, e)

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

  // scalastyle:off
  def enrichTopLevelEvent(dto: BaseEventDto): Future[BaseEventDto] = {
    val t = EventTypeRegistry.unsafeFromId(dto.eventTypeId)
    val query = for {
      actors <- eventActorsTable.filter(_.eventId === dto.id).result
      objects <- t match {
                  case MoveObjectType => DBIO.successful(Seq.empty)
                  case MoveNodeType   => DBIO.successful(Seq.empty)
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

  private def allEventsQuery(eid: Option[EventId] = None) = {
    val fromIdCond = eid.map(e => s"""AND e.EVENT_ID > ${e.underlying}""").getOrElse("")
    sql"""
      SELECT
        e.EVENT_ID, e.EVENT_TYPE_ID, e.EVENT_DATE, e.NOTE, e.PART_OF,
        e.VALUE_LONG, e.VALUE_STRING, e.VALUE_FLOAT, e.REGISTERED_BY, e.REGISTERED_DATE,
        era.ROLE_ID, era.ACTOR_UUID,
        erp.ROLE_ID, erp.PLACE_ID,
        erpo.ROLE_ID, erpo.PLACE_ID,
        ero.ROLE_ID, ero.OBJECT_ID,
        t.MUSITTHING_UUID, t.MUSEUMID
      FROM
        MUSARK_STORAGE.EVENT e
        LEFT OUTER JOIN MUSARK_STORAGE.EVENT_ROLE_ACTOR era
        ON e.EVENT_ID = era.EVENT_ID
        LEFT OUTER JOIN MUSARK_STORAGE.EVENT_ROLE_PLACE erp
        ON e.EVENT_ID=erp.EVENT_ID
        LEFT OUTER JOIN MUSARK_STORAGE.EVENT_ROLE_PLACE_AS_OBJECT erpo
        ON e.EVENT_ID=erpo.EVENT_ID
        LEFT OUTER JOIN MUSARK_STORAGE.EVENT_ROLE_OBJECT ero
        ON e.EVENT_ID=ero.EVENT_ID
        LEFT OUTER JOIN MUSIT_MAPPING.MUSITTHING t
        ON ero.OBJECT_ID=t.OBJECT_ID
      WHERE e.EVENT_TYPE_ID IN (1, 2, 3, 4, 5)
      #${fromIdCond}
      ORDER BY
        e.EVENT_ID ASC,
        erp.ROLE_ID DESC
      """.as[AllTupleType]
  }

  def streamAllEventsFrom(e: EventId): DatabasePublisher[AllEventsRow] = {
    logger.error(s"Starting to read EventIds > ${e.underlying}")
    db.stream(allEventsQuery(Some(e)), bufferNext = true).mapResult(toDto)
  }

  def streamAllEvents: DatabasePublisher[AllEventsRow] =
    db.stream(allEventsQuery(), bufferNext = true).mapResult(toDto)

  def getAllEvents: Future[Seq[AllEventsRow]] =
    db.run(allEventsQuery()).map(_.map(toDto))

  def enrichRelations(
      base: BaseEventDto,
      rowTuple: AllEventsRow
  ): BaseEventDto = {
    val rowBase    = rowTuple._1
    val relActors  = base.relatedActors ++ rowBase.relatedActors
    val relPlaces  = base.relatedPlaces ++ rowBase.relatedPlaces
    val relObjects = base.relatedObjects ++ rowBase.relatedObjects

    base.copy(
      relatedActors = relActors.distinct,
      relatedPlaces = relPlaces.distinct,
      relatedObjects = relObjects.distinct
    )
  }

  private def toDto(row: AllTupleType): AllEventsRow = {
    val eid   = Option(EventId(row._1))
    val etype = EventTypeRegistry.unsafeFromId(EventTypeId(row._2))

    val relActors = row._11.map { rid =>
      Seq(EventRoleActor(eid, rid, ActorId.unsafeFromString(row._12.get)))
    }.getOrElse(Seq.empty)

    val relPlaces = row._13.map { rid =>
      Seq(
        EventRolePlace(
          eid,
          rid,
          StorageNodeDatabaseId.fromOptLong(row._14).get,
          etype.id
        )
      )
    }.getOrElse(Seq.empty)

    val relObjects = {
      if (row._15.isDefined) {
        row._15.map { rid =>
          Seq(EventRoleObject(eid, rid, ObjectId.fromOptLong(row._16).get, etype.id))
        }
      } else {
        row._17.map { rid =>
          Seq(EventRoleObject(eid, rid, ObjectId.fromOptLong(row._18).get, etype.id))
        }
      }
    }.getOrElse(Seq.empty)

    val objectUUID = row._19.flatMap(ObjectUUID.fromString)
    val museumId   = row._20.map(MuseumId.fromInt)

    val base = BaseEventDto(
      id = eid,
      eventTypeId = etype.id,
      eventDate = row._3,
      relatedActors = relActors,
      relatedObjects = relObjects,
      relatedPlaces = relPlaces,
      note = row._4,
      relatedSubEvents = Seq.empty,
      partOf = EventId.fromOptLong(row._5),
      valueLong = row._6,
      valueString = row._7,
      valueDouble = row._8,
      registeredBy = row._9.flatMap(ActorId.fromString),
      registeredDate = row._10
    )

    (base, objectUUID, museumId)
  }

  def addMigratedEvents(ids: Seq[EventId]) = {
    db.run(migratedTable ++= ids).map(r => r).recover {
      case NonFatal(ex) =>
        val msg = s"Could not persist old eventIds for migrated events [$ids]."
        logger.warn(msg, ex)
        throw ex
    }
  }

  def lastMigratedEvent: Future[Option[EventId]] =
    db.run(migratedTable.sortBy(_.oldId.asc).max.result)

  /**
   * Definition of table keeping track of the events that have been migrated.
   */
  class MigratedEventsTable(
      val tag: Tag
  ) extends Table[EventId](tag, Some("MUSARK_STORAGE"), "MIGRATED_EVENTS") {

    val oldId = column[EventId]("OLD_EVENT_ID", O.PrimaryKey)

    def * = oldId

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
