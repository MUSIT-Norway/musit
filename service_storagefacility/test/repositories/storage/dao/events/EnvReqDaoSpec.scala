package repositories.storage.dao.events

import no.uio.musit.models.{EventId, MuseumId}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import utils.testhelpers.{BaseDummyData, EventGenerators}

class EnvReqDaoSpec
    extends MusitSpecWithAppPerSuite
    with BaseDummyData
    with EventGenerators
    with MusitResultValues {

  val dao = fromInstanceCache[EnvReqDao]

  "The EnvReqDao" when {

    "working with environment requirements" should {

      "successfully insert a new environment requirement" in {
        val mid = MuseumId(2)
        val er  = createEnvRequirement(Some(defaultNodeId))
        dao.insert(mid, er).futureValue.successValue mustBe EventId(1L)
      }

      "return the environment requirement associated with the provided id" in {
        val mid = MuseumId(2)
        val er  = createEnvRequirement(Some(defaultNodeId))

        val eid = dao.insert(mid, er).futureValue.successValue
        eid mustBe EventId(2L)

        val res = dao.findById(mid, eid).futureValue.successValue.value

        res.eventType mustBe er.eventType
        res.note mustBe er.note
        res.registeredBy mustBe Some(defaultActorId)
        res.registeredDate must not be None
        res.light mustBe er.light
        res.temperature mustBe er.temperature
        res.hypoxicAir mustBe er.hypoxicAir
        res.airHumidity mustBe er.airHumidity
        res.cleaning mustBe er.cleaning
      }
    }
  }

}
