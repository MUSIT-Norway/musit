package services.old

import com.google.inject.Inject
import models.storage.event.EventTypeRegistry.TopLevelEvents.EnvRequirementEventType
import models.storage.event.dto.DtoConverters.EnvReqConverters
import models.storage.event.dto.{EventDto, ExtendedDto}
import models.storage.event.old.envreq.EnvRequirement
import models.storage.nodes.EnvironmentRequirement
import no.uio.musit.MusitResults.{MusitInternalError, MusitResult, MusitSuccess}
import no.uio.musit.models.{EventId, MuseumId, StorageNodeDatabaseId}
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.storage.old_dao.event.{EnvRequirementDao, EventDao}

import scala.concurrent.Future
import scala.util.control.NonFatal

class EnvironmentRequirementService @Inject()(
    val eventDao: EventDao,
    val envRequirementDao: EnvRequirementDao
) {

  val logger = Logger(classOf[EnvironmentRequirementService])

  private val unexpectedType = MusitInternalError(
    "Unexpected DTO type. Expected ExtendedDto with event type EnvRequirement"
  )

  /**
   * Helper method that will fetch the latest environment requirement event for
   * the StorageNodeId found in the affectedThing property of the event.
   * The passed in envReq is then compared against the last registered event.
   *
   * If the 2 compared events are similar, the last registered event is returned.
   * Otherwise a None value is returned.
   *
   * @param envReq The environment requirement to compare
   * @return A Future containing an Option of the EnvRequirement that was found.
   */
  private def compareWithLatest(
      mid: MuseumId,
      envReq: EnvRequirement
  ): Future[Option[EnvRequirement]] = {
    envReq.affectedThing.map { snid =>
      latestForNodeId(mid, snid)
        .map(_.map { mer =>
          if (mer.exists(_.similar(envReq))) mer
          else None
        }.getOrElse(None))
        .recover {
          // If the attempt to fetch the last environment requirement event for
          // the specific nodeId failed, we're going to write a new event
          case NonFatal(ex) => None
        }
    }.getOrElse {
      /*
        ¡¡¡NOTE!!!: Since environmentRequirement is an Optional argument on the
        StorageNode types, we _have_ to assume they are not modified if they are
        not passed in. Meaning we do _not_ add a new event for it. This should
        be revisited. See TODO in StorageNode.
       */
      Future.successful(None)
    }
  }

  /**
   * TODO: Document me!!!!
   */
  def add(
      mid: MuseumId,
      er: EnvRequirement
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[EnvRequirement]] = {
    val envReq = er.copy(registeredBy = Option(currUsr.id))
    val dto    = EnvReqConverters.envReqToDto(envReq)

    compareWithLatest(mid, envReq).flatMap { sameEr =>
      sameEr.map { er =>
        logger.debug(
          "Did not add new EnvRequirement event because it was " +
            "similar as the previous entry"
        )
        Future.successful(MusitSuccess(er))
      }.getOrElse {
        eventDao.insertEvent(mid, dto).flatMap { eventId =>
          eventDao.getEvent(mid, eventId).map { res =>
            res.flatMap(_.map { dto =>
              // We know we have an ExtendedDto representing an EnvRequirement
              val extDto = dto.asInstanceOf[ExtendedDto]
              MusitSuccess(EnvReqConverters.envReqFromDto(extDto))
            }.getOrElse {
              logger.error(
                s"Unexpected error when trying to fetch an environment" +
                  s" requirement event that was added with eventId $eventId"
              )
              MusitInternalError("Could not locate the EnvRequirement that was added")
            })
          }
        }
      }
    }
  }

  /**
   * TODO: Document me!!!!
   */
  def findBy(mid: MuseumId, id: EventId): Future[MusitResult[Option[EnvRequirement]]] = {
    eventDao.getEvent(mid, id).map { result =>
      convertResult(result)
    }
  }

  /**
   * Helper to locate the last registered environment requirement event for the
   * given StorageNodeId.
   *
   * @param nodeId StorageNodeId to use when looking for the latest event.
   * @return {{{Future[MusitResult[Option[EnvRequirement]]]}}}
   */
  private def latestForNodeId(
      mid: MuseumId,
      nodeId: StorageNodeDatabaseId
  ): Future[MusitResult[Option[EnvRequirement]]] = {
    eventDao.latestByNodeId(mid, nodeId, EnvRequirementEventType.id).map { result =>
      convertResult(result)
    }
  }

  /**
   * Same {{{latestForNodeId}}}, except that this method will do additional
   * mapping from EnvRequirement => EnvironmentRequirement.
   *
   * @param nodeId StorageNodeId to use when looking for the latest event.
   * @return {{{Future[MusitResult[Option[EnvironmentRequirement]]]}}}
   */
  def findLatestForNodeId(
      mid: MuseumId,
      nodeId: StorageNodeDatabaseId
  ): Future[MusitResult[Option[EnvironmentRequirement]]] = {
    eventDao.latestByNodeId(mid, nodeId, EnvRequirementEventType.id).map { result =>
      convertResult(result).map { maybeEvt =>
        maybeEvt.map(EnvRequirement.fromEnvRequirementEvent)
      }
    }
  }

  private def convertResult(result: MusitResult[Option[EventDto]]) = {
    result.flatMap(_.map {
      case ext: ExtendedDto =>
        MusitSuccess(
          Option(EnvReqConverters.envReqFromDto(ext))
        )

      case _ =>
        unexpectedType

    }.getOrElse(MusitSuccess(None)))
  }

}
