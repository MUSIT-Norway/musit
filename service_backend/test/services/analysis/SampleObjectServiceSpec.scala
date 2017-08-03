package services.analysis

import java.util.UUID

import models.analysis.LeftoverSamples.NotSpecified
import models.analysis.SampleStatuses.SampleStatus
import models.analysis._
import models.analysis.events.SampleCreated
import no.uio.musit.MusitResults.MusitSuccess
import no.uio.musit.models.ObjectTypes.{CollectionObjectType, ObjectType}
import no.uio.musit.models._
import no.uio.musit.security._
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.{DateTimeMatchers, MusitResultValues}
import no.uio.musit.time.dateTimeNow

class SampleObjectServiceSpec
    extends MusitSpecWithAppPerSuite
    with MusitResultValues
    with DateTimeMatchers {

  val defaultUserId = ActorId.generate()
  val defaultMid    = Museums.Test.id

  val allCollections = Seq(
    MuseumCollection(
      uuid = CollectionUUID(UUID.fromString("2e4f2455-1b3b-4a04-80a1-ba92715ff613")),
      name = Some("Arkeologi"),
      oldSchemaNames = Seq(MuseumCollections.Archeology)
    )
  )

  implicit val dummyUser = AuthenticatedUser(
    session = UserSession(uuid = SessionUUID.generate()),
    userInfo = UserInfo(
      id = defaultUserId,
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
        museumId = defaultMid,
        description = None,
        collections = allCollections
      )
    )
  )

  val dummyActorId   = ActorId.generate()
  val dummyActorName = "Dummy User"
  val dummyActorById = dummyActorId
  val origObjId      = ObjectUUID.unsafeFromString("67965e71-27ee-4ef0-ad66-e7e321882f33")

  val service      = fromInstanceCache[SampleObjectService]
  val eventService = fromInstanceCache[AnalysisService]

  def generateSampleObject(
      id: Option[ObjectUUID],
      originatingId: ObjectUUID,
      parentId: Option[ObjectUUID],
      parentobjType: ObjectType = CollectionObjectType,
      isExtracted: Boolean = false,
      status: SampleStatus = SampleStatuses.Intact
  ): SampleObject = {
    val now = dateTimeNow
    SampleObject(
      objectId = id,
      parentObject = ParentObject(parentId, parentobjType),
      isExtracted = isExtracted,
      museumId = Museums.Test.id,
      status = status,
      responsible = Some(dummyActorId),
      doneByStamp = Some(ActorStamp(dummyActorId, now)),
      sampleId = None,
      sampleNum = None,
      externalId = None,
      sampleTypeId = SampleTypeId(1),
      size = Some(Size("cm2", 12.0)),
      container = Some("box"),
      storageMedium = None,
      treatment = None,
      leftoverSample = NotSpecified,
      description = None,
      note = Some("This is a sample note"),
      originatedObjectUuid = originatingId,
      registeredStamp = Some(ActorStamp(dummyActorId, now)),
      updatedStamp = None,
      isDeleted = false
    )
  }

  "The SampleObjectService " should {
    val parentId                    = ObjectUUID.generate()
    var addedId: Option[ObjectUUID] = None

    "successfully add a new sample object" in {
      val so =
        generateSampleObject(
          id = None,
          originatingId = origObjId,
          parentId = Some(parentId),
          isExtracted = true
        )

      val addedRes = service.add(defaultMid, so).futureValue.successValue
      addedRes mustBe an[ObjectUUID]
      addedId = Option(addedRes)
    }

    "find the sample by its uuid" in {
      val found = service.findById(addedId.value).futureValue.successValue.value
      found.isExtracted mustBe true
    }

    "find the sample created event for the parent object" in {
      val sce = eventService.findByObject(defaultMid, parentId).futureValue.successValue
      sce.size mustBe 1
      sce.toList match {
        case theHead :: Nil =>
          theHead.analysisTypeId mustBe SampleCreated.sampleEventTypeId
          theHead.doneBy mustBe Some(dummyActorId)
          theHead.doneDate mustApproximate Some(dateTimeNow)
          theHead.registeredBy mustBe Some(dummyUser.id)
          theHead.registeredDate mustApproximate Some(dateTimeNow)
          theHead.affectedThing mustBe Some(parentId)

        case _ =>
          fail(s"The list contained ${sce.size} elements, expected 1.")
      }
    }

    "delete the sample by its uuid" in {
      val found = service.findById(addedId.value).futureValue.successValue.value
      val so    = service.delete(found.objectId.get).futureValue
      so mustBe MusitSuccess(())
    }

    "not find a deleted sample" in {
      val found = service.findById(addedId.value).futureValue.successValue
      found mustBe None
    }

    "successfully add a new sample object with status 'Degraded' " in {
      val so = generateSampleObject(
        id = None,
        originatingId = origObjId,
        parentId = Some(parentId),
        isExtracted = true,
        status = SampleStatuses.Degraded
      )
      val addedRes = service.add(defaultMid, so).futureValue.successValue
      addedRes mustBe an[ObjectUUID]
      addedId = Option(addedRes)
      val status = service.findById(addedId.get).futureValue
      status.successValue.get.status mustBe SampleStatuses.Degraded
    }

    "copy generated values from origin sample when updating" in {
      val originSo = generateSampleObject(
        id = None,
        originatingId = origObjId,
        parentId = Some(parentId)
      )

      val id: ObjectUUID = service.add(defaultMid, originSo).futureValue.successValue
      val originSavedSo  = service.findById(id).futureValue.successValue.value

      val newSo = generateSampleObject(
        id = Some(id),
        originatingId = origObjId,
        parentId = Some(parentId)
      )

      val updatedSo = service.update(id, newSo).futureValue.successValue.value

      updatedSo.objectId mustBe originSavedSo.objectId
      updatedSo.registeredStamp mustBe originSavedSo.registeredStamp
      updatedSo.isDeleted mustBe originSavedSo.isDeleted
      updatedSo.sampleNum mustBe originSavedSo.sampleNum

    }
  }
}
