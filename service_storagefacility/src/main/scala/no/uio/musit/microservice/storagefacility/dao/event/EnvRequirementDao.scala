package no.uio.musit.microservice.storagefacility.dao.event

import com.google.inject.Inject
import no.uio.musit.microservice.storagefacility.dao.SchemaName
import no.uio.musit.microservice.storagefacility.domain.event.dto.EnvRequirementDto
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future

/**
 * Created by ellenjo on 6/30/16.
 */

/**
 * TODO: Document me!
 */
class EnvRequirementDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends BaseEventDao {

  import driver.api._

  private val EnvRequirementTable = TableQuery[EnvRequirementTable]

  /**
   * TODO: Document me!
   */
  def insertAction(event: EnvRequirementDto): DBIO[Int] =
    EnvRequirementTable += event

  /**
   * TODO: Document me!
   */
  def getEnvRequirement(id: Long): Future[Option[EnvRequirementDto]] =
    db.run(EnvRequirementTable.filter(event => event.id === id).result.headOption)

  private class EnvRequirementTable(
      tag: Tag
  ) extends Table[EnvRequirementDto](tag, SchemaName, "E_ENVIRONMENT_REQUIREMENT") {

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

    def create = (
      id: Option[Long],
      temp: Option[Int],
      tempInterval: Option[Int],
      airHumidity: Option[Int],
      airHumInterval: Option[Int],
      hypoxicAir: Option[Int],
      hypAirInterval: Option[Int],
      cleaning: Option[String],
      light: Option[String]
    ) =>
      EnvRequirementDto(
        id = id,
        temperature = temp,
        tempInterval = tempInterval,
        airHumidity = airHumidity,
        airHumInterval = airHumInterval,
        hypoxicAir = hypoxicAir,
        hypoxicInterval = hypAirInterval,
        cleaning = cleaning,
        light = light
      )

    def destroy(envReq: EnvRequirementDto) =
      Some((
        envReq.id,
        envReq.temperature,
        envReq.tempInterval,
        envReq.airHumidity,
        envReq.airHumInterval,
        envReq.hypoxicAir,
        envReq.hypoxicInterval,
        envReq.cleaning,
        envReq.light
      ))
  }

}

