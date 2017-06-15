package repositories.storage.dao.events

import models.storage.event.EventTypeRegistry.TopLevelEvents.{
  MoveNodeType,
  MoveObjectType
}
import models.storage.event.move.{MoveNode, MoveObject}
import no.uio.musit.models._
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import org.scalatest.Inspectors.forAll
import utils.testdata.{BaseDummyData, EventGenerators}

class MoveDaoSpec
    extends MusitSpecWithAppPerSuite
    with BaseDummyData
    with EventGenerators
    with MusitResultValues {

  val dao = fromInstanceCache[MoveDao]

  "The MoveDao" when {

    "working with objects" should {

      "succeed when moving an object" in {
        val mid = MuseumId(2)
        val moveObj = createMoveObject(
          from = Some(firstNodeId),
          to = secondNodeId
        )

        dao.insert(mid, moveObj).futureValue.successValue mustBe EventId(1L)
      }

      "return a MoveObject event with a specific id" in {
        val mid = MuseumId(2)
        val moveObj = createMoveObject(
          from = Some(secondNodeId),
          to = firstNodeId
        )

        val eid = dao.insert(mid, moveObj).futureValue.successValue
        eid mustBe EventId(2L)

        val res = dao.findById(mid, eid).futureValue.successValue

        res.value match {
          case mo: MoveObject =>
            mo.from mustBe Some(secondNodeId)
            mo.to mustBe firstNodeId
            mo.eventType.registeredEventId mustBe MoveObjectType.id
            mo.doneBy mustBe Some(defaultActorId)
            mo.registeredBy mustBe Some(defaultActorId)
            mo.affectedThing mustBe Some(defaultObjectUUID)
            mo.objectType mustBe ObjectTypes.CollectionObjectType

          case err =>
            fail(s"Expected MoveObject but got ${err.getClass}")
        }
      }

      "return all move events for an object" in {
        val mid = MuseumId(2)
        val res = dao.listForObject(mid, defaultObjectUUID).futureValue

        val events = res.successValue
        events.size mustBe 2
      }

      "move a batch of objects" in {
        val mid = MuseumId(2)

        val oid1 = ObjectUUID.generateAsOpt()
        val oid2 = ObjectUUID.generateAsOpt()
        val oid3 = ObjectUUID.generateAsOpt()
        val oid4 = ObjectUUID.generateAsOpt()

        val mo1 = createMoveObject(oid1, Some(secondNodeId), defaultNodeId)
        val mo2 = createMoveObject(oid2, Some(secondNodeId), defaultNodeId)
        val mo3 = createMoveObject(oid3, Some(secondNodeId), defaultNodeId)
        val mo4 = createMoveObject(oid4, Some(secondNodeId), defaultNodeId)

        val res =
          dao.batchInsertObjects(mid, Seq(mo1, mo2, mo3, mo4)).futureValue.successValue

        res must contain allOf (EventId(3L), EventId(4L), EventId(5L), EventId(6L))
      }

    }

    "working with nodes" should {

      "succeed when moving a node" in {
        val mid = MuseumId(2)
        val moveNode = createMoveNode(
          from = Some(firstNodeId),
          to = secondNodeId
        )

        dao.insert(mid, moveNode).futureValue.successValue mustBe EventId(7L)
      }

      "return a MoveNode event with a specific id" in {
        val mid = MuseumId(2)
        val moveNode = createMoveNode(
          from = Some(firstNodeId),
          to = secondNodeId
        )

        val eid = dao.insert(mid, moveNode).futureValue.successValue
        eid mustBe EventId(8L)

        val res = dao.findById(mid, eid).futureValue.successValue

        res.value match {
          case mn: MoveNode =>
            mn.from mustBe Some(firstNodeId)
            mn.to mustBe secondNodeId
            mn.eventType.registeredEventId mustBe MoveNodeType.id
            mn.doneBy mustBe Some(defaultActorId)
            mn.registeredBy mustBe Some(defaultActorId)
            mn.affectedThing mustBe Some(defaultNodeId)
            mn.objectType mustBe ObjectTypes.Node

          case err =>
            fail(s"Expected MoveObject but got ${err.getClass}")
        }
      }

      "return all move events for an object" in {
        val mid = MuseumId(2)
        val res = dao.listForNode(mid, defaultNodeId).futureValue

        val events = res.successValue
        events.size mustBe 2
      }

      "move a batch of objects" in {
        val mid = MuseumId(2)

        val n1 = StorageNodeId.generateAsOpt()
        val n2 = StorageNodeId.generateAsOpt()
        val n3 = StorageNodeId.generateAsOpt()
        val n4 = StorageNodeId.generateAsOpt()

        val mo1 = createMoveNode(n1, Some(secondNodeId), defaultNodeId)
        val mo2 = createMoveNode(n2, Some(secondNodeId), defaultNodeId)
        val mo3 = createMoveNode(n3, Some(secondNodeId), defaultNodeId)
        val mo4 = createMoveNode(n4, Some(secondNodeId), defaultNodeId)

        val res =
          dao.batchInsertNodes(mid, Seq(mo1, mo2, mo3, mo4)).futureValue.successValue

        res must contain allOf (EventId(9L), EventId(10L), EventId(11L), EventId(12L))
      }

    }
  }

}
