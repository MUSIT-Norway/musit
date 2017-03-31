package migration

import com.google.inject.AbstractModule

/**
 * Guice module that binds data migration specific code to the application
 * life-cycle. If nothing needs migrating, this module should _not_ be enabled.
 */
class DataMigrationModule extends AbstractModule {

  def configure() = {

    bind(classOf[EventMigrator]).asEagerSingleton()

  }

}
