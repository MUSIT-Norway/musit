package repositories.analysis.dao

import models.analysis.events.SampleCreated
import models.analysis.{SampleObject, SampleStatuses}
import no.uio.musit.MusitResults.MusitSuccess
import no.uio.musit.models.ObjectTypes.{CollectionObject, ObjectType}
import no.uio.musit.models.{ActorId, EventId, Museums, ObjectUUID}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import org.joda.time.DateTime
import org.scalatest.Inspectors.forAll

class SampleObjectDaoSpec extends MusitSpecWithAppPerSuite with MusitResultValues {

  val dao: SampleObjectDao = fromInstanceCache[SampleObjectDao]

  val defaultActorId = ActorId.generate()

  def generateSample(
      id: ObjectUUID,
      parentId: Option[ObjectUUID],
      parentobjType: ObjectType,
      isExtracted: Boolean = false
  ): SampleObject = {
    val now = DateTime.now
    SampleObject(
      objectId = Some(id),
      parentObjectId = parentId,
      parentObjectType = parentobjType,
      isExtracted = isExtracted,
      museumId = Museums.Test.id,
      status = SampleStatuses.Intact,
      responsible = ActorId.generateAsOpt(),
      createdDate = Some(now),
      sampleId = None,
      externalId = None,
      sampleType = Some("slize"),
      sampleSubType = Some("age rings"),
      size = Some(12.0),
      sizeUnit = Some("cm2"),
      container = Some("box"),
      storageMedium = None,
      note = Some("This is a sample note"),
      registeredBy = ActorId.generateAsOpt(),
      registeredDate = Some(now),
      updatedBy = None,
      updatedDate = None
    )
  }

  def generateSampleEvent(
      doneBy: Option[ActorId] = None,
      doneDate: Option[DateTime] = None,
      registeredBy: Option[ActorId] = Some(defaultActorId),
      registeredDate: Option[DateTime] = None,
      objectId: Option[ObjectUUID] = ObjectUUID.generateAsOpt(),
      sampleObjectId: Option[ObjectUUID] = ObjectUUID.generateAsOpt()
  ): SampleCreated = {
    val now = DateTime.now
    SampleCreated(
      id = None,
      doneBy = doneBy,
      doneDate = doneDate,
      registeredBy = registeredBy,
      registeredDate = registeredDate,
      objectId = objectId,
      sampleObjectId = sampleObjectId
    )
  }

  "The SampleObjectDao" should {

    "return the object UUID of the inserted SampleObject" in {
      val oid = ObjectUUID.generate()
      val so  = generateSample(oid, None, CollectionObject)
      val res = dao.insert(so).futureValue
      res.successValue mustBe oid
    }

    "find the SampleObject with the given UUID" in {
      val oid = ObjectUUID.generate()
      val sol = generateSample(oid, None, CollectionObject)

      dao.insert(sol).futureValue.isSuccess mustBe true

      val res = dao.findByUUID(oid).futureValue
      res.successValue.value.objectId mustBe Some(oid)
    }

    "return None if no SampleObjects with the given UUID exists" in {
      dao.findByUUID(ObjectUUID.generate()).futureValue mustBe MusitSuccess(None)
    }

    "list all SampleObjects derived from a parent Object" in {
      // Create a few sample objects with derived objects
      val parentId = ObjectUUID.generate()
      val childId1 = ObjectUUID.generate()
      val childId2 = ObjectUUID.generate()
      val childId3 = ObjectUUID.generate()

      val parent = generateSample(id = parentId, None, CollectionObject)
      val child1 =
        generateSample(id = childId1, parentId = Some(parentId), CollectionObject)
      val child2 =
        generateSample(id = childId2, parentId = Some(parentId), CollectionObject)
      val child3 =
        generateSample(id = childId3, parentId = Some(parentId), CollectionObject)

      dao.insert(parent).futureValue.isSuccess mustBe true
      dao.insert(child1).futureValue.isSuccess mustBe true
      dao.insert(child2).futureValue.isSuccess mustBe true
      dao.insert(child3).futureValue.isSuccess mustBe true

      val res = dao.listForParentObject(parentId).futureValue

      res.successValue.size mustBe 3

      forAll(res.successValue) { c =>
        c.parentObjectId mustBe Some(parentId)
        c.objectId must not be empty
        c.objectId must contain oneOf (childId1, childId2, childId3)
      }
    }

    "not return an empty list if there are no derived objects" in {
      val res = dao.listForParentObject(ObjectUUID.generate()).futureValue

      res.successValue mustBe empty
    }

    "successfully update a SampleObject" in {
      val oid = ObjectUUID.generate()
      val so1 = generateSample(oid, None, CollectionObject)
      dao.insert(so1).futureValue.isSuccess mustBe true

      val so2 = so1.copy(sampleId = Some("FOO-1"))

      val res1 = dao.update(so2).futureValue
      res1.successValue mustBe 1L

      val res2 = dao.findByUUID(oid).futureValue
      res2.successValue.value.objectId mustBe Some(oid)
      res2.successValue.value.sampleId mustBe Some("FOO-1")
    }

    "return the object UUID of the inserted SampleObject and it's event" in {
      val oid = ObjectUUID.generate()
      val so  = generateSample(oid, None, CollectionObject, isExtracted = true)
      val se = generateSampleEvent(
        doneBy = so.registeredBy,
        doneDate = so.registeredDate,
        registeredBy = so.registeredBy,
        registeredDate = so.registeredDate,
        objectId = so.parentObjectId,
        sampleObjectId = so.objectId
      )
      dao.insert(so, se).futureValue.successValue mustBe oid
    }

  }

}
