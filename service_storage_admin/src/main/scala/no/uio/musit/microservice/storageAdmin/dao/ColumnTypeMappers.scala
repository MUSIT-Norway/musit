package no.uio.musit.microservice.storageAdmin.dao

/**
  * Created by jarle on 20.09.16.
  */

import no.uio.musit.microservice.storageAdmin.domain.NodePath
import play.api.db.slick.HasDatabaseConfig
import slick.driver.JdbcProfile

/**
* Working with some of the DAOs require implicit mappers to/from strongly
* typed value types/classes.
*/
trait ColumnTypeMappers {
  self: HasDatabaseConfig[JdbcProfile] =>

  import driver.api._


}