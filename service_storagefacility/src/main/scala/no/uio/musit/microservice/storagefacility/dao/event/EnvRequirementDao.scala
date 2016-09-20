package no.uio.musit.microservice.storagefacility.dao.event

import com.google.inject.{ Inject, Singleton }
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
@Singleton
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

    def * = (id, temp, tempTolerance, relativeHumidity, relativeHumidityTolerance, hypoxicAir, hypoxicAirTolerance, cleaning, light) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[Option[Long]]("ID", O.PrimaryKey)

    val temp = column[Option[Double]]("TEMPERATURE")
    val tempTolerance = column[Option[Double]]("TEMP_TOLERANCE")
    val relativeHumidity = column[Option[Double]]("REL_HUMIDITY")
    val relativeHumidityTolerance = column[Option[Double]]("REL_HUM_TOLERANCE")
    val hypoxicAir = column[Option[Double]]("HYPOXIC_AIR")
    val hypoxicAirTolerance = column[Option[Double]]("HYP_AIR_TOLERANCE")
    val cleaning = column[Option[String]]("CLEANING")
    val light = column[Option[String]]("LIGHT")

    def create = (
      id: Option[Long],
      temp: Option[Double],
      tempTolerance: Option[Double],
      relHumidity: Option[Double],
      relHumidityTolerance: Option[Double],
      hypoxicAir: Option[Double],
      hypoxicAirTolerance: Option[Double],
      cleaning: Option[String],
      light: Option[String]
    ) =>
      EnvRequirementDto(
        id = id,
        temperature = temp,
        tempInterval = tempTolerance,
        airHumidity = relHumidity,
        airHumInterval = relHumidityTolerance,
        hypoxicAir = hypoxicAir,
        hypoxicInterval = hypoxicAirTolerance,
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

