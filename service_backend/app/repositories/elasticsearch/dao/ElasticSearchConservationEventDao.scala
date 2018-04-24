package repositories.elasticsearch.dao

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.{Inject, Singleton}
import models.conservation.events._
import models.elasticsearch.{
  ExistingConservationSearchObject,
  DeletedConservationSearchObject,
  DeletedOrExistingConservationSearchObject
}
import models.elasticsearch.ExistingConservationSearchObject._
import no.uio.musit.MusitResults.{MusitInternalError, MusitResult, MusitSuccess}
import no.uio.musit.functional.Extensions._
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models._
import no.uio.musit.repositories.events.EventActions
import no.uio.musit.security.AuthenticatedUser
import org.joda.time.DateTime
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import repositories.conservation.dao.{
  ConservationEventRowMappers,
  ConservationEventTableProvider,
  ConservationProcessDao,
  ConservationTables
}
import slick.jdbc.{GetResult, ResultSetConcurrency, ResultSetType}
import java.sql.{Timestamp => JsSqlTimestamp}

import no.uio.musit.models.MuseumCollections.Collection

import scala.concurrent.{ExecutionContext, Future}
@Singleton
class ElasticSearchConservationEventDao @Inject()(
    implicit val dbConfigProvider: DatabaseConfigProvider,
    ec: ExecutionContext,
    val conservationProcessDao: ConservationProcessDao
) extends ConservationEventTableProvider
    with ConservationTables
    with EventActions
    with ConservationEventRowMappers {

  val asyncFetchParallelismCount: Int = 4 //TODO: 4 is arbitrarily chosen

  val logger = Logger(classOf[ElasticSearchConservationEventDao])

  import profile.api._

  type ConservationEventStreamRow =
    (EventId, EventTypeId, MuseumId, Option[CollectionId], Boolean)

  implicit val s: GetResult[ConservationEventStreamRow] = GetResult { p =>
    (
      EventId.fromLong(p.nextLong()),
      EventTypeId(p.nextInt()),
      MuseumId(p.nextInt()),
      p.nextIntOption().map(CollectionId(_)),
      p.nextBoolean()
    )
  }

  private def makeEventTypeProvider(eventTypes: Seq[ConservationType]) = {
    val eventTypeMap: Map[EventTypeId, ConservationType] =
      eventTypes.map(eventType => eventType.id -> eventType).toMap
    (eventTypeId: EventTypeId) =>
      eventTypeMap.get(eventTypeId)
  }

  def conservationEventStream(
      eventsAfterDate: Option[DateTime],
      fetchSize: Int,
      eventTypes: Seq[ConservationType],
      eventProvider: (
          EventId,
          EventTypeId,
          MuseumId
      ) => FutureMusitResult[ConservationModuleEvent]
  ): Source[MusitResult[DeletedOrExistingConservationSearchObject], NotUsed] = {

    val dateClause = eventsAfterDate match {
      case Some(date) => {
        val ts = new JsSqlTimestamp(date.getMillis).toString
        s""" WHERE (ev.UPDATED_DATE >= {ts '$ts'}
           | OR ev.REGISTERED_DATE >= {ts '$ts'})""".stripMargin
      }
      case None => ""
    }

    val query =
      sql"""SELECT ev.event_id, ev.type_id, ev.MUSEUM_ID,
      | min(t.NEW_COLLECTION_ID) collection_id, ev.IS_DELETED FROM MUSARK_CONSERVATION.EVENT ev 
      | LEFT OUTER JOIN MUSARK_CONSERVATION.OBJECT_EVENT oe ON oe.EVENT_ID = ev.EVENT_ID
      | LEFT OUTER JOIN MUSIT_MAPPING.MUSITTHING t ON oe.OBJECT_UUID = t.MUSITTHING_UUID
      | #${dateClause}
      | GROUP BY ev.event_id, ev.type_id, ev.MUSEUM_ID, ev.IS_DELETED""".stripMargin
        .as[ConservationEventStreamRow]

//gammel:    WHERE ev.IS_DELETED=0 #${dateClause}

    val eventTypeProvider = makeEventTypeProvider(eventTypes)

    Source
      .fromPublisher(
        db.stream(
          query
            .withStatementParameters(
              rsType = ResultSetType.ForwardOnly,
              rsConcurrency = ResultSetConcurrency.ReadOnly,
              fetchSize = fetchSize
            )
            .transactionally //I don't know whether this one is needed, but Ingar used it so I use it as well.
        )
      )
      .mapAsyncUnordered(asyncFetchParallelismCount) { x =>
        {
          val optCollectionId   = x._4
          val optCollectionUuid = optCollectionId.map(Collection.fromInt(_)).map(_.uuid)
          if (x._5) { //isDeleted true, then we can't fetch the event
            val deleted: DeletedOrExistingConservationSearchObject =
              DeletedConservationSearchObject(
                x._1,
                x._3,
                optCollectionUuid
              )

            Future.successful(MusitSuccess(deleted))

          } else {
            eventProvider(x._1, x._2, x._3)
              .mapAndFlattenMusitResult(
                event => {
                  val eventTypeId = event.eventTypeId
                  eventTypeProvider(eventTypeId) match {
                    case Some(eventType) =>
                      MusitSuccess(
                        ExistingConservationSearchObject(
                          x._3,
                          optCollectionUuid,
                          event,
                          eventType,
                          None
                        )
                      )
                    case None =>
                      MusitInternalError(
                        s"Event with unknown event typeId: $eventTypeId for event with id: ${event.id}"
                      )
                  }
                }
              )
              .value
          }
        }
      }
  }

  def defaultEventProvider(
      eventId: EventId,
      eventTypeId: EventTypeId,
      museumId: MuseumId
  ): FutureMusitResult[ConservationModuleEvent] = {

    val res: FutureMusitResult[Option[ConservationModuleEvent]] =
      if (eventTypeId == ConservationProcess.eventTypeId) {
        conservationProcessDao
          .findConservationProcessIgnoreSubEvents(museumId, eventId)(
            AuthenticatedUser.backendUser
          )
          .asInstanceOf[FutureMusitResult[Option[ConservationModuleEvent]]]

      } else {

        conservationProcessDao
          .readSubEvent(eventTypeId, museumId, eventId)(AuthenticatedUser.backendUser)
          .asInstanceOf[FutureMusitResult[Option[ConservationModuleEvent]]]
      }
    res.getOrError(
      MusitInternalError(
        s"Unable to read event with id: $eventId, type: $eventTypeId, museumId: $museumId"
      )
    )
  }
}
