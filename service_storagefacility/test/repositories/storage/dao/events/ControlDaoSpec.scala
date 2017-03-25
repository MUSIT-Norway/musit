package repositories.storage.dao.events

import models.storage.event.EventType
import models.storage.event.EventTypeRegistry.TopLevelEvents.ControlEventType
import no.uio.musit.models._
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import utils.testhelpers.{BaseDummyData, EventGenerators}

class ControlDaoSpec
    extends MusitSpecWithAppPerSuite
    with BaseDummyData
    with EventGenerators
    with MusitResultValues {

  val dao = fromInstanceCache[ControlDao]

  "The ControlDao" when {

    "working with controls" should {
      "successfully insert a new control" in {
        val mid  = MuseumId(2)
        val ctrl = createControl(Some(defaultNodeId))
        dao.insert(mid, ctrl).futureValue.successValue mustBe EventId(1L)
      }

      "return the control associated with the provided id" in {
        val mid  = MuseumId(2)
        val ctrl = createControl(Some(defaultNodeId))

        val eid = dao.insert(mid, ctrl).futureValue.successValue
        eid mustBe EventId(2L)

        val res = dao.findById(mid, eid).futureValue.successValue.value

        res.eventType mustBe EventType.fromEventTypeId(ControlEventType.id)
        res.registeredBy mustBe Some(defaultActorId)
        res.registeredDate must not be empty
        res.alcohol mustBe ctrl.alcohol
        res.cleaning mustBe ctrl.cleaning
        res.pest mustBe ctrl.pest
        res.relativeHumidity mustBe ctrl.relativeHumidity
        res.mold mustBe ctrl.mold
        res.gas mustBe ctrl.gas
        res.hypoxicAir mustBe ctrl.hypoxicAir
        res.lightingCondition mustBe ctrl.lightingCondition
      }
    }
  }

}
