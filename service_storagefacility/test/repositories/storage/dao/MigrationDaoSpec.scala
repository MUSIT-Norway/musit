package repositories.storage.dao

import akka.stream.scaladsl.Source
import no.uio.musit.models.EventId
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import utils.testhelpers.{EventGenerators_Old, MigrationTest, NodeGenerators}

import scala.concurrent.Await
import scala.concurrent.duration._

// TODO: This can be removed when Migration has been performed.

class MigrationDaoSpec
    extends MusitSpecWithAppPerSuite
    with NodeGenerators
    with EventGenerators_Old
    with MusitResultValues
    with MigrationTest {

  val migDao = fromInstanceCache[MigrationDao]

  val total = Await.result(bootstrap(), 1 minute) + 1

  "MigrationDao" should {

    "grouping results from a left outer join should leave 261 distinct events" in {
      val s = migDao.streamAllEvents

      Source
        .fromPublisher(s)
        .runFold[List[EventId]](List.empty) { (state, curr) =>
          val currId = curr._1.id.get
          if (state.contains(currId)) state
          else state :+ currId
        }
        .futureValue
        .size mustBe 261
    }
  }

}
