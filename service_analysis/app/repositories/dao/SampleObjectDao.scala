package repositories.dao

import com.google.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider

@Singleton
class SampleObjectDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends Tables {

  import driver.api._

}
