package no.uio.musit.microservice.event.dao

import no.uio.musit.microservice.event.service.MoveObject
import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by jarle on 22.08.16.
 */

object MoveObjectDao extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  //  private val EventActorsTable = TableQuery[EventActorsTable]

  def executeMove(newEventId: Long, moveObject: MoveObject): DBIO[Unit] = {
    //egentlige inserte i object tabellen, cache inn siste_hid (newEventId) og stedet.
    val res = sql"""select 1 from MUSARK_EVENT.EVENT""".as[Long].head

    res.map(_ => ())
  }
}

