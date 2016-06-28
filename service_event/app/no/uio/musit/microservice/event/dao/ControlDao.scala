package no.uio.musit.microservice.event.dao

import no.uio.musit.microservice.event.domain.{ Control, ControlDTO }
import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import slick.driver.JdbcProfile

import scala.concurrent.Future

/**
 * Created by ellenjo on 6/28/16.
 */
object ControlDAO extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val ControlTable = TableQuery[ControlTable]

  def insertAction(newId: Long, event: Control): DBIO[Int] = {
    val dtoToInsert = event.controlDTO.copy(id = Some(newId))
    val action = ControlTable += dtoToInsert
    action
  }

  def getControl(id: Long): Future[Option[ControlDTO]] = {
    val action = ControlTable.filter(event => event.id === id).result.headOption
    db.run(action)
  }

  private class ControlTable(tag: Tag) extends Table[ControlDTO](tag, Some("MUSARK_EVENT"), "CONTROL") {
    def * = (id, controlType) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[Option[Long]]("ID", O.PrimaryKey)

    val controlType = column[Option[String]]("CONTROLTYPE")

    def create = (id: Option[Long], controlType: Option[String]) =>
      ControlDTO(
        id, controlType
      )

    def destroy(event: ControlDTO) = Some(event.id, event.controlType)
  }

}