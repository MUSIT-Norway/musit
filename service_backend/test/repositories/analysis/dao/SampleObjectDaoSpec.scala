package repositories.analysis.dao

import java.util.UUID

import models.analysis._
import models.storage.event.EventTypeRegistry.TopLevelEvents.MoveObjectType
import models.storage.event.StorageFacilityEventType
import models.storage.event.move.MoveObject
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.models.ObjectTypes.{CollectionObjectType, SampleObjectType}
import no.uio.musit.models._
import no.uio.musit.security._
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import no.uio.musit.time.dateTimeNow
import org.scalatest.Inspectors.forAll
import repositories.storage.dao.events.MoveDao
import utils.testdata.SampleObjectGenerators

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SampleObjectDaoSpec
    extends MusitSpecWithAppPerSuite
    with MusitResultValues
    with SampleObjectGenerators {

  val dao: SampleObjectDao = fromInstanceCache[SampleObjectDao]

  // Necessary for testing functionality to list samples for node
  val moveDao: MoveDao = fromInstanceCache[MoveDao]

  val collections = Seq(
    MuseumCollection(
      uuid = CollectionUUID(UUID.fromString("2e4f2455-1b3b-4a04-80a1-ba92715ff613")),
      name = Some("Arkeologi"),
      oldSchemaNames = Seq(MuseumCollections.Archeology)
    )
  )

  implicit val dummyUser = AuthenticatedUser(
    session = UserSession(
      uuid = SessionUUID.generate(),
      oauthToken = Option(BearerToken(UUID.randomUUID().toString)),
      userId = Option(defaultActorId),
      isLoggedIn = true
    ),
    userInfo = UserInfo(
      id = defaultActorId,
      secondaryIds = Some(Seq("vader@starwars.com")),
      name = Some("Darth Vader"),
      email = None,
      picture = None
    ),
    groups = Seq(
      GroupInfo(
        id = GroupId.generate(),
        name = "FooBarGroup",
        module = StorageFacility,
        permission = Permissions.Admin,
        museumId = mid,
        description = None,
        collections = collections
      )
    )
  )

  def placeSamplesInNode(
      mid: MuseumId,
      nodeId: StorageNodeId,
      samples: Seq[ObjectUUID]
  ) = {
    val events = samples.map { sampleId =>
      MoveObject(
        id = None,
        doneBy = Some(defaultActorId),
        doneDate = Some(dateTimeNow),
        affectedThing = Some(sampleId),
        registeredBy = Some(defaultActorId),
        registeredDate = Some(dateTimeNow),
        eventType = StorageFacilityEventType.fromEventTypeId(MoveObjectType.id),
        objectType = ObjectTypes.SampleObjectType,
        from = None,
        to = nodeId
      )
    }

    moveDao.batchInsertObjects(mid, events)
  }

  def insert(samples: SampleObject*): Seq[MusitResult[ObjectUUID]] = {
    samples.map(s => dao.insert(s).futureValue)
  }

  "The SampleObjectDao" should {

    "return the object UUID of the inserted SampleObject" in {
      val oid = ObjectUUID.generate()
      val so  = generateSample(oid, None, CollectionObjectType)
      val res = dao.insert(so).futureValue
      res.successValue mustBe oid
    }

    "find the SampleObject with the given UUID" in {
      val oid = ObjectUUID.generate()
      val sol = generateSample(oid, None, CollectionObjectType)

      dao.insert(sol).futureValue.isSuccess mustBe true

      val res = dao.findByUUID(oid).futureValue
      res.successValue.value.objectId mustBe Some(oid)
    }

    "return None if no SampleObjects with the given UUID exists" in {
      dao.findByUUID(ObjectUUID.generate()).futureValue mustBe MusitSuccess(None)
    }

    "list all SampleObjects derived from a parent" in {
      // Create a few sample objects with derived objects
      val parentId = ObjectUUID.generate()
      val childId1 = ObjectUUID.generate()
      val childId2 = ObjectUUID.generate()
      val childId3 = ObjectUUID.generate()

      val s1 = generateSample(id = parentId, None, CollectionObjectType)
      val s2 = generateSample(childId1, Some(parentId), CollectionObjectType, parentId)
      val s3 = generateSample(childId2, Some(parentId), CollectionObjectType, parentId)
      val s4 = generateSample(childId3, Some(childId2), SampleObjectType, parentId)

      forAll(insert(s1, s2, s3, s4))(_.isSuccess mustBe true)

      val res = dao.listForParentObject(parentId).futureValue

      res.successValue.size mustBe 2

      forAll(res.successValue) { c =>
        c.parentObject.objectId mustBe Some(parentId)
        c.objectId must not be empty
        c.objectId must contain oneOf (childId1, childId2, childId3)
      }
    }

    "list all SampleObjects derived from the same originating collection object" in {
      val origId = ObjectUUID.generate()
      val sid1   = ObjectUUID.generate()
      val sid2   = ObjectUUID.generate()
      val sid3   = ObjectUUID.generate()
      val sid4   = ObjectUUID.generate()
      val sid5   = ObjectUUID.generate()
      val sid6   = ObjectUUID.generate()

      val s1 = generateSample(sid1, Some(origId), CollectionObjectType, origId)
      val s2 = generateSample(sid2, Some(origId), CollectionObjectType, origId)
      val s3 = generateSample(sid3, Some(sid1), CollectionObjectType, origId)
      val s4 = generateSample(sid4, Some(sid1), CollectionObjectType, origId)
      val s5 = generateSample(sid5, Some(sid4), CollectionObjectType, origId)
      val s6 = generateSample(sid6, Some(sid5), CollectionObjectType, origId)

      forAll(insert(s1, s2, s3, s4, s5, s6))(_.isSuccess mustBe true)

      val res = dao.listForOriginatingObject(origId).futureValue

      res.successValue.size mustBe 6

      forAll(res.successValue) { c =>
        c.originatedObjectUuid mustBe origId
        c.objectId must not be empty
        c.objectId must contain oneOf (sid1, sid2, sid3, sid4, sid5, sid6)
      }
    }

    "not return an empty list if there are no derived objects" in {
      val res = dao.listForParentObject(ObjectUUID.generate()).futureValue

      res.successValue mustBe empty
    }

    "successfully update a SampleObject" in {
      val oid = ObjectUUID.generate()
      val so1 = generateSample(oid, None, CollectionObjectType)
      dao.insert(so1).futureValue.isSuccess mustBe true

      val so2 = so1.copy(sampleId = Some("FOO-1"))

      val res1 = dao.update(so2).futureValue
      res1.successValue must equal(())

      val res2 = dao.findByUUID(oid).futureValue
      res2.successValue.value.objectId mustBe Some(oid)
      res2.successValue.value.sampleId mustBe Some("FOO-1")
    }

    "successfully update a SampleObject without incrementing the sampleNum" in {
      val oid    = ObjectUUID.generate()
      val origin = generateSample(oid, None, CollectionObjectType)

      dao.insert(origin).futureValue.successValue
      val insertedSample = dao.findByUUID(oid).futureValue.successValue.value

      val updated = insertedSample.copy(note = Some("FOO-1"))
      dao.update(updated).futureValue.successValue

      val updatedResult = dao.findByUUID(oid).futureValue.successValue.value
      updatedResult.sampleNum mustBe insertedSample.sampleNum
    }

    "return the object UUID of the inserted SampleObject and it's event" in {
      val oid = ObjectUUID.generate()
      val so  = generateSample(oid, None, CollectionObjectType)
      val se = generateSampleEvent(
        doneBy = so.registeredStamp.map(r => r.user),
        doneDate = so.registeredStamp.map(_.date),
        registeredBy = so.registeredStamp.map(_.user),
        registeredDate = so.registeredStamp.map(_.date),
        objectId = so.parentObject.objectId,
        sampleObjectId = so.objectId
      )
      dao.insert(mid, so, se).futureValue.successValue mustBe oid
      val res2 = dao.findByUUID(oid).futureValue.successValue.value
      res2.sampleTypeId mustBe SampleTypeId(1)
    }

    "successfully delete a SampleObject" in {
      val oid = ObjectUUID.generate()
      val so1 = generateSample(oid, None, CollectionObjectType)
      dao.insert(so1).futureValue.isSuccess mustBe true
      val oldIsDeleted = so1.isDeleted
      oldIsDeleted mustBe false

      val so2  = so1.copy(isDeleted = true)
      val res1 = dao.update(so2).futureValue
      res1.successValue must equal(())

      val res2 = dao.findByUUID(oid).futureValue
      res2.successValue mustBe None
    }

    "successfully list all samples on a given node" in {
      val origObjId =
        ObjectUUID.unsafeFromString("7de44f6e-51f5-4c90-871b-cef8de0ce93d")
      val destNode =
        StorageNodeId.unsafeFromString("6e5b9810-9bbf-464a-a0b9-c27f6095ba0c")

      val oid1 = ObjectUUID.generate()
      val oid2 = ObjectUUID.generate()
      val oid3 = ObjectUUID.generate()

      val samples = Seq.newBuilder[SampleObject]
      samples += generateSample(
        oid1,
        Some(origObjId),
        CollectionObjectType,
        origObjectId = origObjId
      )
      samples += generateSample(
        oid2,
        Some(origObjId),
        CollectionObjectType,
        origObjectId = origObjId
      )
      samples += generateSample(
        oid3,
        Some(oid2),
        SampleObjectType,
        origObjectId = origObjId
      )

      // First we need to insert some samples
      val insRes = Future.sequence(samples.result().map(s => dao.insert(s))).futureValue
      forAll(insRes)(r => r.isSuccess mustBe true)

      // Now we need to move them to a specific node
      placeSamplesInNode(
        mid = mid,
        nodeId = destNode,
        samples = Seq(oid1, oid2, oid3)
      ).futureValue.isSuccess mustBe true

      val res =
        dao.listForNode(mid, destNode, collections).futureValue.successValue

      res.size mustBe 3
      forAll(res) { r =>
        r.museumNo mustBe MuseumNo("C1")
        r.subNo mustBe Some(SubNo("13"))
        r.term mustBe "Fin øks"
        r.sampleObject.originatedObjectUuid mustBe origObjId
      }

      val resSamples = res.map(_.sampleObject)

      resSamples.flatMap(_.objectId) must contain allOf (oid1, oid2, oid3)
    }

  }

}
