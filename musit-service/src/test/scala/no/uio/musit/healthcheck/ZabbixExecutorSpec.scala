package no.uio.musit.healthcheck

import java.io.FileInputStream
import java.nio.file.{Files, Paths}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import no.uio.musit.test.MusitSpec
import org.joda.time.{DateTime, DateTimeUtils}
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.concurrent.Future

class ZabbixExecutorSpec extends MusitSpec with BeforeAndAfterEach {

  override def afterEach() = {
    DateTimeUtils.setCurrentMillisSystem()
  }

  implicit val actorSystem  = ActorSystem("ZabbixExecutorSpec")
  implicit val materializer = ActorMaterializer()

  val meta = ZabbixMeta(
    name = "musit-dev",
    instance = "localhost",
    url = "http://localhost/buildinfo",
    hostGroup = "musit-developer"
  )

  "ZabbixExecutor" when {
    "write results to file" in {
      val folder     = createTempFolder()
      val zibbixFile = ZabbixFile(folder, "musit-health.json")

      val executor: ZabbixExecutor = createExecutor(zibbixFile)
      val result                   = executor.executeHealthChecks().futureValue

      val res = Json.parse(new FileInputStream(zibbixFile.ensureWritableFile()))
      (res \ "noop").as[Boolean] mustBe true
    }

    "write result over old file" in {
      val folder                   = createTempFolder()
      val zibbixFile               = ZabbixFile(folder, "musit-health.json")
      val executor: ZabbixExecutor = createExecutor(zibbixFile)
      val currentTime              = DateTime.now()
      val latestUpdated            = currentTime.plusHours(1).getMillis

      DateTimeUtils.setCurrentMillisFixed(currentTime.getMillis)
      executor.executeHealthChecks().futureValue

      DateTimeUtils.setCurrentMillisFixed(latestUpdated)
      executor.executeHealthChecks().futureValue

      val res = Json.parse(new FileInputStream(zibbixFile.ensureWritableFile()))
      (res \ "updated").as[Long] mustBe latestUpdated
    }

    "write result over old file even when the state changes" in {
      val folder     = createTempFolder()
      val zibbixFile = ZabbixFile(folder, "musit-health.json")
      val executor: ZabbixExecutor =
        createExecutor(zibbixFile, new FlippingAvailableHealthCheck)

      (1 to 3).foreach { n =>
        executor.executeHealthChecks().futureValue
        val res = Json.parse(new FileInputStream(zibbixFile.ensureWritableFile()))
        res mustBe a[JsObject]
      }
    }
  }

  def createExecutor(
      zabbixFile: ZabbixFile,
      healthCheck: HealthCheck = new NoopHealthCheck
  ) = {
    val executor = new ZabbixExecutor(
      zabbixMeta = meta,
      healthChecks = Set(healthCheck),
      zabbaxFile = zabbixFile,
      actorSystem = actorSystem,
      materializer = materializer
    )
    executor.close()
    executor
  }

  def createTempFolder(): String = {
    val tempFolderName = s"scalatest-${System.currentTimeMillis()}"
    val tmpDir         = Paths.get(System.getProperty("java.io.tmpdir"))
    val directory      = Files.createTempDirectory(tmpDir, tempFolderName)

    directory.toString
  }
}

class NoopHealthCheck() extends HealthCheck {
  override def healthCheck() = Future.successful(
    HealthCheckStatus(
      name = "noop",
      available = true,
      responseTime = 1,
      message = None
    )
  )
}

class FlippingAvailableHealthCheck() extends HealthCheck {
  var available = true

  override def healthCheck() = {
    available = !available
    Future.successful(
      HealthCheckStatus(
        name = "flipping",
        available = available,
        responseTime = 1,
        message = None
      )
    )
  }
}
