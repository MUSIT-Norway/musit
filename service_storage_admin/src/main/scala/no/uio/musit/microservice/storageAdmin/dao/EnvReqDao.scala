package no.uio.musit.microservice.storageAdmin.dao

import com.google.inject.{ Inject, Singleton }
import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig, HasDatabaseConfigProvider }
import slick.driver.JdbcProfile
import no.uio.musit.microservice.storageAdmin.domain.{ EnvironmentRequirement, Storage }
import no.uio.musit.microservice.storageAdmin.domain.dto.{ EnvReqDto, StorageDtoConverter }

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Created by ellenjo on 08.09.16.
 */

@Singleton
class EnvReqDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider

) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  private val EnvReqTable = TableQuery[EnvReqTable]

  def insertAction(event: EnvReqDto): DBIO[EnvReqDto] =

    EnvReqTable returning EnvReqTable.map(_.id) into
      ((eventX, id) =>
        eventX.copy(id = id)) += event

  def insertAction(optEvent: Option[EnvReqDto]): DBIO[Option[EnvReqDto]] =
    optEvent match {
      case None => DBIO.successful(None)
      case Some(event) => insertAction(event).map(Some(_))
    }
  /*

  def updateStorageNodeLatestEnvReq(storageNodeId: Int, newEventId: Long): DBIO[Unit] = {
    /* TODO: When service_storageAdmin and service_event has been merged, activate this code (also need to import StorageNodeTable)
    val q = for {
      l <- StorageNodeTable if l.storageNodeId === storageNodeId
    } yield (l.latestEntReqId)
    q.update(Some(newEventId)).map(_ => ())
    */
    DBIO.successful(())
  }

  def execute(newEventId: Long, envRequirement: EnvRequirement): DBIO[Unit] = {
    require(envRequirement.relatedObjects.length <= 1, "More than one objectId in executeMovePlace.")

    val optPlaceAsObjectAndRelation = envRequirement.relatedObjects.headOption
    optPlaceAsObjectAndRelation match {
      case None => throw new Exception("Missing place/storageNode to create environmentRequirement for.")
      case Some(placeAsObjectAndRelation) =>
        updateStorageNodeLatestEnvReq(placeAsObjectAndRelation.objectId.toInt, newEventId)
    }
  }
*/

  def getById(id: Long): Future[Option[EnvReqDto]] =
    db.run(EnvReqTable.filter(event => event.id === id).result.headOption)

  private class EnvReqTable(tag: Tag) extends Table[EnvReqDto](tag, Some("MUSARK_STORAGE"), "E_ENVIRONMENT_REQUIREMENT") {
    def * = (id, temperature, tempTolerance, relativeHumidity, relativeHumidityTolerance, hypoxicAir, hypAirTolerance,
      cleaning, lightingCond, note) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[Option[Long]]("ID", O.PrimaryKey, O.AutoInc)
    val temperature = column[Option[Double]]("TEMPERATURE")
    val tempTolerance = column[Option[Double]]("TEMPERATURE_TOLERANCE")
    val relativeHumidity = column[Option[Double]]("RELATIVE_HUMIDITY")
    val relativeHumidityTolerance = column[Option[Double]]("REL_HUM_TOLERANCE")
    val hypoxicAir = column[Option[Double]]("HYPOXIC_AIR")
    val hypAirTolerance = column[Option[Double]]("HYP_AIR_TOLERANCE")
    val cleaning = column[Option[String]]("CLEANING")
    val lightingCond = column[Option[String]]("LIGHTING_COND")
    val note = column[Option[String]]("NOTE")

    def create = (id: Option[Long],
      temp: Option[Double],
      temp_interval: Option[Double],
      air_humidity: Option[Double],
      air_hum_interval: Option[Double],
      hypoxic_air: Option[Double],
      hyp_air_interval: Option[Double],
      cleaning: Option[String],
      light: Option[String],
      note: Option[String]) =>
      EnvReqDto(
        id = id,
        temperature = temp,
        temperatureTolerance = temp_interval,
        relativeHumidity = air_humidity,
        relativeHumidityTolerance = air_hum_interval,
        hypoxicAir = hypoxic_air,
        hypoxicAirTolerance = hyp_air_interval,
        cleaning = cleaning,
        lightingCond = light,
        note = note

      )

    def destroy(envReq: EnvReqDto) = Some(envReq.id, envReq.temperature, envReq.temperatureTolerance, envReq.relativeHumidity, envReq.relativeHumidityTolerance,
      envReq.hypoxicAir, envReq.hypoxicAirTolerance, envReq.cleaning, envReq.lightingCond, envReq.note)
  }

}
