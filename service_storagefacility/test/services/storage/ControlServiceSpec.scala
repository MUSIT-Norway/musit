package services.storage

import models.storage.event.control.Control
import no.uio.musit.models.{EventId, MuseumId}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import org.scalatest.Inspectors.forAll
import utils.testhelpers.{BaseDummyData, EventGenerators, NodeGenerators}

class ControlServiceSpec
    extends MusitSpecWithAppPerSuite
    with BaseDummyData
    with NodeGenerators
    with EventGenerators
    with MusitResultValues {

  val service = fromInstanceCache[ControlService]

  val nodeId = defaultBuilding.nodeId.get // It's set..so OK to .get
  val ctrl   = createControl(Some(nodeId))

  def assertControl(actual: Control, expected: Control) = {
    actual.affectedThing mustBe expected.affectedThing
    actual.alcohol mustBe expected.alcohol
    actual.cleaning mustBe expected.cleaning
    actual.gas mustBe expected.gas
    actual.hypoxicAir mustBe expected.hypoxicAir
    actual.lightingCondition mustBe expected.lightingCondition
    actual.mold mustBe expected.mold
    actual.pest mustBe expected.pest
    actual.temperature mustBe expected.temperature
    actual.relativeHumidity mustBe expected.relativeHumidity
  }

  "The ControlService" should {

    "successfully save a new Control" in {
      val res = service.add(defaultMuseumId, nodeId, ctrl).futureValue.successValue
      res.id.value mustBe EventId(1L)
      assertControl(res, ctrl)
    }

    "fail when inserting a Control with the wrong museumId" in {
      val anotherMid = MuseumId(4)
      service.add(anotherMid, nodeId, ctrl).futureValue.isFailure mustBe true
    }

    "find a Control with a specific Id" in {
      val res = service.findBy(defaultMuseumId, EventId(1L)).futureValue.successValue
      res.value.id.value mustBe EventId(1L)
      assertControl(res.value, ctrl)
    }

    "list all Control events for a given node" in {
      val nid1  = defaultRoom.nodeId.get
      val nid2  = defaultStorageUnit.nodeId.get
      val ctrl1 = createControl(Some(nid1))
      val ctrl2 = createControl(Some(nid2))
      val ctrl3 = createControl(Some(nodeId))

      val r1 = service.add(defaultMuseumId, nid1, ctrl1).futureValue.successValue
      val r2 = service.add(defaultMuseumId, nid2, ctrl2).futureValue.successValue
      val r3 = service.add(defaultMuseumId, nodeId, ctrl3).futureValue.successValue

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
