import dao.ObjectAggregationDao
import models.MusitResults.MusitSuccess
import models.{ MuseumIdentifier, ObjectId }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Millis, Seconds, Span }
import org.scalatestplus.play.{ OneAppPerSuite, PlaySpec }
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{ Application, Logger }
import testHelpers.TestConfigs
import testHelpers.TestConfigs.WaitLonger

class ObjectAggregationDaoSpec extends PlaySpec with OneAppPerSuite with ScalaFutures with WaitLonger {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  implicit override lazy val app = new GuiceApplicationBuilder()
    .configure(TestConfigs.inMemoryDatabaseConfig())
    .build()

  val dao: ObjectAggregationDao = {
    val instance = Application.instanceCache[ObjectAggregationDao]
    instance(app)
  }

  "Interacting with the ObjectAggregationDao" when {

    "getting objects for a nodeId that exists" should {
      "return a list of objects" in {
        val mr = dao.getObjects(3)
        val fut = mr.futureValue
        fut match {
          case MusitSuccess(result) =>
            result match {
              case Vector(first, second, third) =>
                first.id mustBe ObjectId(1)
                first.identifier mustBe MuseumIdentifier("C666", Some("34"))
                first.displayName mustBe Some("Øks")

                second.id mustBe ObjectId(2)
                second.identifier mustBe MuseumIdentifier("C666", Some("31"))
                second.displayName mustBe Some("Sverd")

                third.id mustBe ObjectId(3)
                third.identifier mustBe MuseumIdentifier("C666", Some("38"))
                third.displayName mustBe Some("Sommerfugl")
            }
          case _ =>
            Logger.error("something went wrong")
            fail("This went TOTALLY off the road")
        }
      }
    }

    "getting objects for a nodeId that does not exist" should {
      "return a an empty vector" in {
        val mr = dao.getObjects(999999)
        val fut = mr.futureValue
        fut match {
          case MusitSuccess(result) =>
            result.length mustBe 0
          case _ => fail("Should fail")
        }
      }
    }
  }
}

