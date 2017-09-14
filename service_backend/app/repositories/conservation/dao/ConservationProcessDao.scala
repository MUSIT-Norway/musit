package repositories.conservation.dao

import com.google.inject.{Inject, Singleton}
import no.uio.musit.repositories.events.EventActions
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext
@Singleton
class ConservationProcessDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext
) extends ConservationEventTableProvider
    with ConservationTables
    //with EventActions
    //with ConservationEventRowMappers
    {

  val logger = Logger(classOf[ConservationProcessDao])

  import profile.api._

}
