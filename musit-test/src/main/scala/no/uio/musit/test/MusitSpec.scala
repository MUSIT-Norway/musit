package no.uio.musit.test

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatestplus.play._
import org.scalatestplus.play.guice.{
  GuiceOneAppPerSuite,
  GuiceOneAppPerTest,
  GuiceOneServerPerTest
}
import play.api.Application
import play.api.libs.ws.WSClient
import play.api.test.TestServer

import scala.util.Properties.envOrNone

/**
 * Base trait to use
 */
trait MusitSpec extends PlaySpec with ScalaFutures {
  val timeout  = envOrNone("MUSIT_FUTURE_TIMEOUT").map(_.toDouble).getOrElse(5 * 1000d)
  val interval = envOrNone("MUSIT_FUTURE_INTERVAL").map(_.toDouble).getOrElse(15d)

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(timeout, Millis),
    interval = Span(interval, Millis)
  )

}

/**
 * Mixin this trait if all you need is a fake Play application.
 */
trait MusitSpecWithApp extends MusitSpec with MusitFakeApplication {

  // ¡¡¡NOTE: Do not make this immutable!!!
  var musitFakeApp = createApplication()

  def fromInstanceCache[T](implicit manifest: Manifest[T]): T = {
    val instance = Application.instanceCache[T]
    instance(musitFakeApp)
  }

}

/**
 * Mixin this trait if you need a fresh fake Play application per test in a
 * *Spec.scala file.
 */
trait MusitSpecWithAppPerTest extends MusitSpecWithApp with GuiceOneAppPerTest {
  implicit override def newAppForTest(testData: TestData): Application = {
    musitFakeApp = createApplication()
    musitFakeApp
  }

}

/**
 * Mixin this trait if you need a fresh fake Play application for all tests in a
 * *Spec.scala file. This is also the one to use if you
 * bundle tests into a bigger Suite involving more than 1 file.
 */
trait MusitSpecWithAppPerSuite extends MusitSpecWithApp with GuiceOneAppPerSuite {
  implicit override lazy val app = musitFakeApp
}

/**
 * For integration testing you will need to mixin a running server with a fake
 * application. Use this trait to have 1 server start/stop per test in a
 * *Spec.scala file.
 */
trait MusitSpecWithServerPerTest
    extends MusitSpecWithApp
    with Network
    with GuiceOneServerPerTest {

  override lazy val port: Int = generatePort

  implicit override def newAppForTest(testData: TestData): Application = {
    musitFakeApp = createApplication()
    musitFakeApp
  }
}

/**
 * For integration testing you will need to mixin a running server with a fake
 * application. Use this trait to have 1 server start/stop per  *Spec.scala
 * file. This is also to be used when bundling specs in a bigger Suite of tests.
 */
trait MusitSpecWithServerPerSuite
    extends MusitSpecWithApp
    with Network
    with SuiteMixin
    with ServerProvider {
  this: Suite =>

  override lazy val port: Int = generatePort

  implicit override lazy val app = musitFakeApp
  implicit lazy val wsClient     = fromInstanceCache[WSClient]

  def beforeTests(): Unit = ()

  def afterTests(): Unit = ()

  /**
   * Overriding the default run method in OneServerPerSuite to be able to pre-
   * load test data for the test-scenario.
   */
  override def run(testName: Option[String], args: Args): Status = {
    val testServer = TestServer(port, app)
    testServer.start()

    beforeTests()

    try {
      val newConfigMap = args.configMap +
        ("org.scalatestplus.play.app"  -> app) +
        ("org.scalatestplus.play.port" -> port)

      val newArgs = args.copy(configMap = newConfigMap)
      val status  = super.run(testName, newArgs)
      status.whenCompleted { _ =>
        afterTests()
        testServer.stop()
      }
      status
    } catch { // In case the suite aborts, ensure the server is stopped
      case ex: Throwable =>
        afterTests()
        testServer.stop()
        throw ex // scalastyle:ignore
    }
  }

}
