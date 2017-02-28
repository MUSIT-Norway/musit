package repositories.dao

import models.events.AnalysisEvent
import no.uio.musit.MusitResults.MusitSuccess
import no.uio.musit.models.{ActorId, EventId}
import no.uio.musit.time.dateTimeNow
import no.uio.musit.test.MusitSpecWithAppPerSuite

class AnalysisEventDaoSpec extends MusitSpecWithAppPerSuite {

  val dao: AnalysisEventDao = fromInstanceCache[AnalysisEventDao]

  val dummyActorId = ActorId.generate()

  "AnalysisEventDao" when {

    "inserting new events" should {
      "return the EventId allocated to a stored general event" in {
        val now = dateTimeNow
        val e: AnalysisEvent = GeneralEvent(
          id = None,
          eventType = EventTypes.LightReactions,
          eventDate = now,
          registeredBy = Some(dummyActorId),
          registeredDate = Some(now),
          note = Some("This is the first even"),
          externalReference = Some("123123")
        )

        dao.insert(e).futureValue mustBe MusitSuccess(EventId(1))
      }

    }

    "fetching events" should {
      "return the event that matches the given id" in {
        val res = dao.findById(EventId(1)).futureValue
        res.isSuccess mustBe true
        res.get must not be empty
        res.get.get.registeredBy mustBe Some(dummyActorId)
        res.get.get.eventType mustBe EventTypes.LightReactions
      }
    }

  }

}
