package no.uio.musit.healthcheck

import java.io.File
import java.nio.file.StandardOpenOption.{CREATE, TRUNCATE_EXISTING, WRITE}
import java.nio.file.Files

import akka.actor.ActorSystem
import akka.stream.{IOResult, Materializer}
import akka.stream.scaladsl.{FileIO, Flow, Keep, Source}
import akka.util.ByteString
import org.joda.time.DateTime
import play.api.Mode.Mode
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.{Configuration, Logger, Mode}

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class ZabbixExecutor(
    zabbixMeta: ZabbixMeta,
    healthChecks: Set[HealthCheck],
    zabbaxFile: ZabbixFile,
    actorSystem: ActorSystem,
    materializer: Materializer
) {

  val logger = Logger(classOf[ZabbixExecutor])

  logger.info("Setting up health check in interval")

  implicit val system = actorSystem
  implicit val mat    = materializer

  val scheduler = actorSystem.scheduler.schedule(
    initialDelay = 10 seconds,
    interval = 4 minutes
  ) {
    executeHealthChecks().onFailure {
      case t => logger.warn("Failed to execute health check", t)
    }
  }

  def close(): Unit = {
    scheduler.cancel()
  }

  def executeHealthChecks(): Future[IOResult] = {
    Future
      .sequence(healthChecks.map(_.healthCheck()))
      .map(
        hc =>
          Zabbix(
            meta = zabbixMeta,
            updated = DateTime.now(),
            healthChecks = hc
        )
      )
      .flatMap(z => writeToFile(z))
  }

  private def writeToFile(z: Zabbix): Future[IOResult] = {
    val sink = Flow[String]
      .map(s => ByteString(s))
      .toMat(
        FileIO.toPath(
          zabbaxFile.ensureWritableFile().toPath,
          Set(CREATE, WRITE, TRUNCATE_EXISTING)
        )
      )(Keep.right)

    Source.fromFuture(Future.successful(Json.prettyPrint(z.toJson))).runWith(sink)
  }

}

object ZabbixExecutor {

  def apply(
      buildInfoName: String,
      healthCheckEndpoint: String,
      healthChecks: Set[HealthCheck],
      environmentMode: Mode,
      configuration: Configuration
  )(implicit actorSystem: ActorSystem, materializer: Materializer): ZabbixExecutor = {
    val zabbixFilePath = environmentMode match {
      case Mode.Prod => "/opt/docker/zabbix/"
      case _         => "./target/"
    }
    Files.createDirectories(new File(zabbixFilePath).toPath)

    def resolveStringConfiguration(key: String) =
      configuration.getString(key).toRight(key).right

    val meta = for {
      env      <- resolveStringConfiguration("musit.env")
      baseUrl  <- resolveStringConfiguration("musit.baseUrl")
      hostname <- resolveStringConfiguration("musit.docker.hostname")
    } yield
      (
        ZabbixFile(zabbixFilePath, s"musit-$buildInfoName-$env-health.json"),
        ZabbixMeta(
          s"musit-$buildInfoName-$env",
          s"$hostname-$buildInfoName",
          s"$baseUrl/$healthCheckEndpoint",
          "siteadmin-uav-itf-ds"
        )
      )

    meta match {
      case Right((p, m)) =>
        new ZabbixExecutor(
          zabbixMeta = m,
          healthChecks = healthChecks,
          zabbaxFile = p,
          actorSystem = actorSystem,
          materializer = materializer
        )
      case Left(key) =>
        throw new IllegalStateException(s"Missing configuration with key '$key'")
    }
  }

}
