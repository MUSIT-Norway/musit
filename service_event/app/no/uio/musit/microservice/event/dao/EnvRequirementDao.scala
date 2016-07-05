package no.uio.musit.microservice.event.dao

import no.uio.musit.microservice.event.domain.Dto
import no.uio.musit.microservices.common.domain.MusitInternalErrorException
import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import play.api.libs.json.Json
import slick.driver.JdbcProfile

import scala.concurrent.Future

/**
 * Created by ellenjo on 6/30/16.
 */

object EnvRequirementDAO extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val EnvRequirementTable = TableQuery[EnvRequirementTable]

  case class EnvRequirementDto(id: Option[Long], temperature: Option[Int], tempInterval: Option[Int], airHumidity: Option[Int],
      airHumInterval: Option[Int], hypoxicAir: Option[Int], hypoxicInterval: Option[Int],
      cleaning: Option[String], light: Option[String]) extends Dto {

    //def validateInterval = if(tempInterval.get > 0) tempInterval else throw new MusitInternalErrorException("Can't have negative intervals")
  }

  object EnvRequirementDto {
    implicit val format = Json.format[EnvRequirementDto]
  }

  /*def valueLongToOptBool = valueLong match {
    case Some(1) => Some(true)
    case Some(0) => Some(false)
    case None => None
    case n => throw new MusitInternalErrorException(s"Wrong boolean value $n")
  }

  def valueLongToBool = valueLongToOptBool match {
    case Some(b) => b
    case None => throw new MusitInternalErrorException("Missing boolean value")
  }
}*/

  /*object EnvRequirementDto {
  def fromEvent(evt: Event) = EventBaseDto(evt.id, evt.links, evt.eventType, evt.note)
}*/
  def insertAction(event: EnvRequirementDto): DBIO[Int] =
    EnvRequirementTable += event

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

