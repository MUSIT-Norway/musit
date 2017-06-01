package no.uio.musit.healthcheck

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.{ScalaModule, ScalaMultibinder}

class FakeHealthCheckModule extends AbstractModule with ScalaModule {
  override def configure() = {
    ScalaMultibinder.newSetBinder[HealthCheck](binder)
  }
}
