package services.storage

import models.storage.event.envreq.EnvRequirement
import no.uio.musit.models.{EventId, MuseumId}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import utils.testhelpers.{BaseDummyData, EventGenerators, NodeGenerators}

class EnvironmentRequirementServiceSpec
    extends MusitSpecWithAppPerSuite
    with BaseDummyData
    with NodeGenerators
    with EventGenerators
    with MusitResultValues {

  val service = fromInstanceCache[EnvironmentRequirementService]

  val nodeId = defaultBuilding.nodeId.get // It's set..so OK to .get
  val er     = createEnvRequirement(Some(nodeId))

  def assertEnvRequirement(actual: EnvRequirement, expected: EnvRequirement) = {
    actual.affectedThing.value mustBe nodeId
    actual.airHumidity mustBe expected.airHumidity
    actual.cleaning mustBe expected.cleaning
    actual.hypoxicAir mustBe expected.hypoxicAir
    actual.light mustBe expected.light
    actual.note mustBe expected.note
    actual.temperature mustBe expected.temperature
  }

  "The EnvironmentRequirementService" should {

    "successfully save a new EnvRequirement" in {
      val res = service.add(defaultMuseumId, er).futureValue.successValue

      res.id.value mustBe EventId(1L)
      assertEnvRequirement(res, er)
    }

    "fail when inserting a EnvRequirement with the wrong museumId" in {
      /*
        TODO:
        Currently this service doesn't check for validity of the node that the
        event is registered for. That isn't really correct. There should be 2 ways
        to add an EnvRequirement.

        1. A function that takes MuseumId, target nodeId and the EnvRequirement event
        2. A function that takes MuseumId and EnvRequirement event.

        Where the second function should _not_ be exposed outside the service package.
        It should scoped to only be callable by other services where the existence of
        the node has already been determined. Perhaps it should even take the node as
        argument to be 100% sure of the validity.
       */
      val anotherMid = MuseumId(4)
      val res        = service.add(anotherMid, er).futureValue

      res.isFailure mustBe true
    }

    "find an EnvRequirement with a specific Id" in {
      val res = service.findBy(defaultMuseumId, EventId(1L)).futureValue.successValue
      res.value.id.value mustBe EventId(1L)
      assertEnvRequirement(res.value, er)
    }

    "find the latest EnvironmentRequirement for a given node" in {
      val res = service.findLatestForNodeId(nodeId).futureValue

      val r = res.successValue.value
      r.cleaning mustBe er.cleaning
      r.comment mustBe er.note
      r.hypoxicAir mustBe er.hypoxicAir
      r.lightingCondition mustBe er.light
      r.relativeHumidity mustBe er.airHumidity
      r.temperature mustBe er.temperature
    }
  }

}
