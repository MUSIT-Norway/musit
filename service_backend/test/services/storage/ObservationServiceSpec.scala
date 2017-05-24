package services.storage

import models.storage.event.observation.Observation
import no.uio.musit.models.{EventId, MuseumId}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import org.scalatest.Inspectors.forAll
import utils.testdata.{BaseDummyData, EventGenerators, NodeGenerators}

class ObservationServiceSpec
    extends MusitSpecWithAppPerSuite
    with BaseDummyData
    with NodeGenerators
    with EventGenerators
    with MusitResultValues {

  val service = fromInstanceCache[ObservationService]

  val nodeId = defaultBuilding.nodeId.get // It's set..so OK to .get
  val obs    = createObservation(Some(nodeId))

  def assertObservation(actual: Observation, expected: Observation) = {
    actual.affectedThing mustBe expected.affectedThing
    actual.alcohol mustBe expected.alcohol
    actual.cleaning mustBe expected.cleaning
    actual.gas mustBe expected.gas
    actual.pest mustBe expected.pest
    actual.mold mustBe expected.mold
    actual.hypoxicAir mustBe expected.hypoxicAir
    actual.temperature mustBe expected.temperature
    actual.relativeHumidity mustBe expected.relativeHumidity
    actual.lightingCondition mustBe expected.lightingCondition
    actual.perimeterSecurity mustBe expected.perimeterSecurity
    actual.fireProtection mustBe expected.fireProtection
    actual.theftProtection mustBe expected.theftProtection
    actual.waterDamageAssessment mustBe expected.waterDamageAssessment
  }

  "The ObservationService" should {
    "successfully save a new Observation" in {
      val res = service.add(defaultMuseumId, nodeId, obs).futureValue.successValue
      res.id.value mustBe EventId(1L)
      assertObservation(res, obs)
    }

    "fail when inserting a Observation with the wrong museumId" in {
      val anotherMid = MuseumId(4)
      service.add(anotherMid, nodeId, obs).futureValue.isFailure mustBe true
    }

    "find an Observation with a specific Id" in {
      val res = service.findBy(defaultMuseumId, EventId(1L)).futureValue.successValue
      res.value.id.value mustBe EventId(1L)
      assertObservation(res.value, obs)
    }

    "list all Observation events for a given node" in {
      val nid1 = defaultRoom.nodeId.get
      val nid2 = defaultStorageUnit.nodeId.get
      val obs1 = createObservation(Some(nid1))
      val obs2 = createObservation(Some(nid2))
      val obs3 = createObservation(Some(nodeId))

      val r1 = service.add(defaultMuseumId, nid1, obs1).futureValue.successValue
      val r2 = service.add(defaultMuseumId, nid2, obs2).futureValue.successValue
      val r3 = service.add(defaultMuseumId, nodeId, obs3).futureValue.successValue

      r1.id.value mustBe EventId(2L)
      r2.id.value mustBe EventId(3L)
      r3.id.value mustBe EventId(4L)

      val res = service.listFor(defaultMuseumId, nodeId).futureValue.successValue
      res.size mustBe 2
      res.map(_.id.value) must contain allOf (EventId(1L), EventId(4L))
      forAll(res.map(_.affectedThing)) { t =>
        t.value mustBe nodeId
      }
    }
  }

}
