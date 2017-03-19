package repositories.storage.dao.events

import com.google.inject.{Inject, Singleton}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import repositories.storage.dao.EventTables

@Singleton
class EnvReqDao @Inject()(val dbConfigProvider: DatabaseConfigProvider)
    extends EventTables {

  import profile.api._

  val logger = Logger(classOf[EnvReqDao])

}
