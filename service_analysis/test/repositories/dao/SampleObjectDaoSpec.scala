package repositories.dao

import models.{SampleObject, SampleStatuses}
import no.uio.musit.models.{ActorId, Museums, ObjectUUID}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.joda.time.DateTime

class SampleObjectDaoSpec extends MusitSpecWithAppPerSuite {

  val dao: SampleObjectDao = fromInstanceCache[SampleObjectDao]

  def generateSample(
    id: ObjectUUID,
    parentId: Option[ObjectUUID],
    isColObj: Boolean
  ) = {
    val now = DateTime.now
    SampleObject(
      objectId = Some(id),
      parentObjectId = parentId,
      isCollectionObject = isColObj,
      museumId = Museums.Test.id,
      status = SampleStatuses.Ok,
      responsible = ActorId.generate(),
      createdDate = now,
      sampleNumber = None,
      externalId = None,
      note = Some("This is a sample note"),
      updatedBy = None,
      updatedDate = None
    )
  }

  "The SampleObjectDao" should {

    "return the object UUID of the inserted SampleObject" in {
      val oid = ObjectUUID.generate()
      val so = generateSample(oid, None, isColObj = false)
      val res = dao.insert(so).futureValue
      res.isSuccess mustBe true
      res.get mustBe oid
    }

    "successfully update a SampleObject" in {
      val oid = ObjectUUID.generate()
      val so1 = generateSample(oid, None, isColObj = false)
      dao.insert(so1).futureValue.isSuccess mustBe true

      val so2 = so1.copy(sampleNumber = Some("FOO-1"))

      val res1 = dao.update(so2).futureValue
      res1.isSuccess mustBe true
      res1.get mustBe 1L

      val res2 = dao.findByUUID(oid).futureValue
      res2.isSuccess mustBe true
      res2.get must not be empty
      res2.get.get.objectId mustBe Some(oid)
      res2.get.get.sampleNumber mustBe Some("FOO-1")
    }

    "find the SampleObject with the given UUID" in {
      pending
    }

    "return None if no SampleObjects with the given UUID exists" in {
      pending
    }

    "list all SampleObjects derived from a parent Object" in {
      pending
    }

  }

}
