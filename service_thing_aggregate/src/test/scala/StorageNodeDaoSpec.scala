import dao.StorageNodeDao
import models.MusitResults.MusitSuccess
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{ OneAppPerSuite, PlaySpec }
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import testHelpers.TestConfigs
import testHelpers.TestConfigs.WaitLonger

class StorageNodeDaoSpec extends PlaySpec with OneAppPerSuite with ScalaFutures with WaitLonger {

  implicit override lazy val app = new GuiceApplicationBuilder()
    .configure(TestConfigs.inMemoryDatabaseConfig())
    .build()

  val dao: StorageNodeDao = {
    val instance = Application.instanceCache[StorageNodeDao]
    instance(app)
  }

  "Interacting with the StorageNodeDao" when {

    "getting objects for a nodeId that does not exist" should {
      "return false" in {
        dao.nodeExists(9999).futureValue match {
          case MusitSuccess(false) =>
          case _ => fail("it should not exist")
        }
      }
    }

    "getting objects for a nodeId that exists" should {
      "return true" in {
        dao.nodeExists(3).futureValue match {
          case MusitSuccess(true) =>
          case _ => fail("it should exist")
        }
      }
    }
  }
}
