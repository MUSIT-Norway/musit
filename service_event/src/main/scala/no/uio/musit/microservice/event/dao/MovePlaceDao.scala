package no.uio.musit.microservice.event.dao

import no.uio.musit.microservice.event.service.MovePlace
import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import slick.driver.JdbcProfile
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by jarle on 22.08.16.
 */

object MovePlaceDao extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  //  private val EventActorsTable = TableQuery[EventActorsTable]

  def executeMove(newEventId: Long, movePlace: MovePlace): DBIO[Unit] = {
    val res = sql"""select 1 from MUSARK_EVENT.EVENT""".as[Long].head
    res.map(_ => ())
  }
}

