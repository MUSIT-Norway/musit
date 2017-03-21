package repositories.storage.dao.events

import models.storage.event.EventTypeRegistry.TopLevelEvents.{
  MoveNodeType,
  MoveObjectType
}
import models.storage.event.move.{MoveNode, MoveObject}
import no.uio.musit.models._
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues

class MoveDaoSpec
    extends MusitSpecWithAppPerSuite
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
            mo.objectType mustBe ObjectTypes.CollectionObject

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

    }

    "working with nodes" should {

      "succeed when moving a node" in {
        val mid = MuseumId(2)
        val moveNode = createMoveNode(
          from = Some(firstNodeId),
          to = secondNodeId
        )

        dao.insert(mid, moveNode).futureValue.successValue mustBe EventId(3L)
      }

      "return a MoveNode event with a specific id" in {
        val mid = MuseumId(2)
        val moveNode = createMoveNode(
          from = Some(firstNodeId),
          to = secondNodeId
        )

        val eid = dao.insert(mid, moveNode).futureValue.successValue
        eid mustBe EventId(4L)

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

    }
  }

}
