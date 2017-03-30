package migration

import com.google.inject.AbstractModule

class DataMigrationModule extends AbstractModule {

  def configure() = {

    bind(classOf[EventMigrator]).asEagerSingleton()

  }

}
