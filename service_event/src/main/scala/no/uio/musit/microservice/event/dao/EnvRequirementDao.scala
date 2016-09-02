package no.uio.musit.microservice.event.dao

import no.uio.musit.microservice.event.domain.{ Dto, EnvRequirementDto }
import no.uio.musit.microservice.event.service.EnvRequirement
import no.uio.musit.microservices.common.domain.MusitInternalErrorException
import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import play.api.libs.json.Json
import slick.driver.JdbcProfile

import scala.concurrent.Future

/**
 * Created by ellenjo on 6/30/16.
 */

object EnvRequirementDao extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val EnvRequirementTable = TableQuery[EnvRequirementTable]

  def insertAction(event: EnvRequirementDto): DBIO[Int] =
    EnvRequirementTable += event

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

  def getEnvRequirement(id: Long): Future[Option[EnvRequirementDto]] =
    db.run(EnvRequirementTable.filter(event => event.id === id).result.headOption)

  private class EnvRequirementTable(tag: Tag) extends Table[EnvRequirementDto](tag, Some("MUSARK_EVENT"), "E_ENVIRONMENT_REQUIREMENT") {
    def * = (id, temp, temp_interval, air_humidity, air_hum_interval, hypoxic_air, hyp_air_interval, cleaning, light) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[Option[Long]]("ID", O.PrimaryKey)

    val temp = column[Option[Int]]("TEMPERATURE")
    val temp_interval = column[Option[Int]]("TEMP_INTERVAL")
    val air_humidity = column[Option[Int]]("AIR_HUMIDITY")
    val air_hum_interval = column[Option[Int]]("AIR_HUM_INTERVAL")
    val hypoxic_air = column[Option[Int]]("HYPOXIC_AIR")
    val hyp_air_interval = column[Option[Int]]("HYP_AIR_INTERVAL")
    val cleaning = column[Option[String]]("CLEANING")
    val light = column[Option[String]]("LIGHT")

    def create = (id: Option[Long], temp: Option[Int], temp_interval: Option[Int], air_humidity: Option[Int],
      air_hum_interval: Option[Int], hypoxic_air: Option[Int], hyp_air_interval: Option[Int],
      cleaning: Option[String], light: Option[String]) =>
      EnvRequirementDto(
        id, temp, temp_interval, air_humidity, air_hum_interval, hypoxic_air, hyp_air_interval, cleaning, light

      )

    def destroy(envReq: EnvRequirementDto) = Some(envReq.id, envReq.temperature, envReq.tempInterval, envReq.airHumidity, envReq.airHumInterval,
      envReq.hypoxicAir, envReq.hypoxicInterval, envReq.cleaning, envReq.light)
  }

}

