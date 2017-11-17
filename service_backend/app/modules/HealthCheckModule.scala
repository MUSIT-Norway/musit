package modules

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.google.inject.{AbstractModule, Inject, Provider}
import controllers.routes
import net.codingwell.scalaguice.{ScalaModule, ScalaMultibinder}
import no.uio.musit.healthcheck.{HealthCheck, HealthCheckDao, ZabbixExecutor}
import no.uio.musit.service.BuildInfo
import play.api.{Configuration, Environment}
import services.elasticsearch.ElasticsearchHealthCheck

class HealthCheckModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    val healthChecks = ScalaMultibinder.newSetBinder[HealthCheck](binder)
    healthChecks.addBinding.to[HealthCheckDao]
    healthChecks.addBinding.to[ElasticsearchHealthCheck]

    bind[ZabbixExecutor].toProvider(classOf[ZabbixExecutorProvider]).asEagerSingleton()
  }

}

class ZabbixExecutorProvider @Inject()(
    implicit environment: Environment,
    configuration: Configuration,
    healthChecks: Set[HealthCheck],
    actorSystem: ActorSystem,
    materializer: Materializer
) extends Provider[ZabbixExecutor] {
  override def get() = ZabbixExecutor(
    BuildInfo.name,
    s"api/${routes.HealthCheckController.healthCheck().url.replaceAll("/service_backend/", "")}",
    healthChecks,
    environment.mode,
    configuration
  )

}
