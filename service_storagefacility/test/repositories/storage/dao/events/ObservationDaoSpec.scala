package repositories.storage.dao.events

import models.storage.event.EventType
import models.storage.event.EventTypeRegistry.TopLevelEvents.ObservationEventType
import no.uio.musit.models.{EventId, MuseumId}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues

class ObservationDaoSpec
    extends MusitSpecWithAppPerSuite
    with EventGenerators
    with MusitResultValues {

  val dao = fromInstanceCache[ObservationDao]

  "The ObservationDao" when {

    "working with controls" should {

      "successfully insert a new observation" in {
        val mid = MuseumId(2)
        val obs = createObservation(Some(defaultNodeId))
        dao.insert(mid, obs).futureValue.successValue mustBe EventId(1L)
      }

      "return the observation associated with the provided id" in {
        val mid = MuseumId(2)
        val obs = createObservation(Some(defaultNodeId))

        val eid = dao.insert(mid, obs).futureValue.successValue
        eid mustBe EventId(2L)

        val res = dao.findById(mid, eid).futureValue.successValue.value

        res.eventType mustBe EventType.fromEventTypeId(ObservationEventType.id)
        res.registeredBy mustBe Some(defaultActorId)
        res.registeredDate must not be None
        res.alcohol mustBe obs.alcohol
        res.cleaning mustBe obs.cleaning
        res.gas mustBe obs.gas
        res.hypoxicAir mustBe obs.hypoxicAir
        res.lightingCondition mustBe obs.lightingCondition
        res.mold mustBe obs.mold
        res.pest mustBe obs.pest
        res.relativeHumidity mustBe obs.relativeHumidity
        res.temperature mustBe obs.temperature
      }
    }
  }

}
