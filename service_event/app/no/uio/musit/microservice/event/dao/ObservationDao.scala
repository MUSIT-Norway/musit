package no.uio.musit.microservice.event.dao

import no.uio.musit.microservice.event.domain.{ Observation, ObservationDTO }
import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import slick.driver.JdbcProfile

import scala.concurrent.Future

/**
 * Created by ellenjo on 6/28/16.
 */

object ObservationDAO extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val ObservationTable = TableQuery[ObservationTable]

  def insertAction(newId: Long, event: Observation): DBIO[Int] = {
    val dtoToInsert = event.observationDTO.copy(id = Some(newId))
    val action = ObservationTable += dtoToInsert
    action
  }

  def getObservation(id: Long): Future[Option[ObservationDTO]] = {
    val action = ObservationTable.filter(event => event.id === id).result.headOption
    db.run(action)
  }

  private class ObservationTable(tag: Tag) extends Table[ObservationDTO](tag, Some("MUSARK_EVENT"), "OBSERVATION") {
    def * = (id, temperature) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[Option[Long]]("ID", O.PrimaryKey)

    val temperature = column[Option[Double]]("TEMPERATURE")

    def create = (id: Option[Long], temperature: Option[Double]) =>
      ObservationDTO(
        id, temperature
      )

    def destroy(event: ObservationDTO) = Some(event.id, event.temperature)
  }

}