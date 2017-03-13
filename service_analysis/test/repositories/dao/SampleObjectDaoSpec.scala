package repositories.dao

import models.{SampleObject, SampleStatuses}
import no.uio.musit.MusitResults.MusitSuccess
import no.uio.musit.models.{ActorId, Museums, ObjectUUID}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.joda.time.DateTime
import org.scalatest.Inspectors.forAll

class SampleObjectDaoSpec extends MusitSpecWithAppPerSuite {

  val dao: SampleObjectDao = fromInstanceCache[SampleObjectDao]

  def generateSample(
    id: ObjectUUID,
    parentId: Option[ObjectUUID],
    isColObj: Boolean = false
  ): SampleObject = {
    val now = DateTime.now
    SampleObject(
      objectId = Some(id),
      parentObjectId = parentId,
      isCollectionObject = isColObj,
      museumId = Museums.Test.id,
      status = SampleStatuses.Ok,
      responsible = ActorId.generate(),
      createdDate = now,
      sampleId = None,
      externalId = None,
      note = Some("This is a sample note"),
      registeredBy = ActorId.generateAsOpt(),
      registeredDate = Some(now),
      updatedBy = None,
      updatedDate = None
    )
  }

  "The SampleObjectDao" should {

    "return the object UUID of the inserted SampleObject" in {
      val oid = ObjectUUID.generate()
      val so = generateSample(oid, None)
      val res = dao.insert(so).futureValue
      res.isSuccess mustBe true
      res.get mustBe oid
    }

    "find the SampleObject with the given UUID" in {
      val oid = ObjectUUID.generate()
      val sol = generateSample(oid, None)

      dao.insert(sol).futureValue.isSuccess mustBe true

      val res = dao.findByUUID(oid).futureValue
      res.isSuccess mustBe true
      res.get must not be empty
      res.get.get.objectId mustBe Some(oid)
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

      val parent = generateSample(id = parentId, None)
      val child1 = generateSample(id = childId1, parentId = Some(parentId))
      val child2 = generateSample(id = childId2, parentId = Some(parentId))
      val child3 = generateSample(id = childId3, parentId = Some(parentId))

      dao.insert(parent).futureValue.isSuccess mustBe true
      dao.insert(child1).futureValue.isSuccess mustBe true
      dao.insert(child2).futureValue.isSuccess mustBe true
      dao.insert(child3).futureValue.isSuccess mustBe true

      val res = dao.listForParentObject(parentId).futureValue

      res.isSuccess mustBe true
      res.get.size mustBe 3

      forAll(res.get) { c =>
        c.parentObjectId mustBe Some(parentId)
        c.objectId must not be empty
        c.objectId must contain oneOf (childId1, childId2, childId3)
      }
    }

    "not return an empty list if there are no derived objects" in {

      val res = dao.listForParentObject(ObjectUUID.generate()).futureValue

      res.isSuccess mustBe true
      res.get mustBe empty
    }

    "successfully update a SampleObject" in {
      val oid = ObjectUUID.generate()
      val so1 = generateSample(oid, None)
      dao.insert(so1).futureValue.isSuccess mustBe true

      val so2 = so1.copy(sampleId = Some("FOO-1"))

      val res1 = dao.update(so2).futureValue
      res1.isSuccess mustBe true
      res1.get mustBe 1L

      val res2 = dao.findByUUID(oid).futureValue
      res2.isSuccess mustBe true
      res2.get must not be empty
      res2.get.get.objectId mustBe Some(oid)
      res2.get.get.sampleId mustBe Some("FOO-1")
    }

  }

}
