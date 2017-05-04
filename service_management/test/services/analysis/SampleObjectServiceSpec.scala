package services.analysis

import models.analysis.LeftoverSamples.NotSpecified
import models.analysis.events.SampleCreated
import models.analysis._
import no.uio.musit.MusitResults.MusitSuccess
import no.uio.musit.models.ObjectTypes.{CollectionObject, ObjectType}
import no.uio.musit.models.{ActorId, Museums, ObjectUUID}
import no.uio.musit.security.{AuthenticatedUser, SessionUUID, UserInfo, UserSession}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.{DateTimeMatchers, MusitResultValues}
import no.uio.musit.time.dateTimeNow

class SampleObjectServiceSpec
    extends MusitSpecWithAppPerSuite
    with MusitResultValues
    with DateTimeMatchers {

  val defaultUserId = ActorId.generate()

  implicit val dummyUser = AuthenticatedUser(
    session = UserSession(uuid = SessionUUID.generate()),
    userInfo = UserInfo(
      id = defaultUserId,
      secondaryIds = Some(Seq("vader@starwars.com")),
      name = Some("Darth Vader"),
      email = None,
      picture = None
    ),
    groups = Seq.empty
  )

  val dummyActorId = ActorId.generate()

  val service      = fromInstanceCache[SampleObjectService]
  val eventService = fromInstanceCache[AnalysisService]

  def generateSampleObject(
      id: Option[ObjectUUID],
      parentId: Option[ObjectUUID],
      parentobjType: ObjectType = CollectionObject,
      isExtracted: Boolean = false
  ): SampleObject = {
    val now = dateTimeNow
    SampleObject(
      objectId = id,
      parentObjectId = parentId,
      parentObjectType = parentobjType,
      isExtracted = isExtracted,
      museumId = Museums.Test.id,
      status = SampleStatuses.Intact,
      responsible = Some(dummyActorId),
      createdDate = Some(now),
      sampleId = None,
      externalId = None,
      sampleType = Some(SampleType("slize", Some("age rings"))),
      size = Some(Size("cm2", 12.0)),
      container = Some("box"),
      storageMedium = None,
      treatment = None,
      leftoverSample = NotSpecified,
      description = None,
      note = Some("This is a sample note"),
      originatedObjectUuid = ObjectUUID.generate(),
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
          parentId = Some(parentId),
          isExtracted = true
        )

      val addedRes = service.add(so).futureValue.successValue
      addedRes mustBe an[ObjectUUID]
      addedId = Option(addedRes)
    }

    "find the sample by its uuid" in {
      val found = service.findById(addedId.value).futureValue.successValue.value
      found.isExtracted mustBe true
    }

    "find the sample created event for the parent object" in {
      val sce = eventService.findByObject(parentId).futureValue.successValue
      sce.size mustBe 1
      sce.toList match {
        case theHead :: Nil =>
          theHead.analysisTypeId mustBe SampleCreated.sampleEventTypeId
          theHead.doneBy mustBe Some(dummyUser.id)
          theHead.doneDate mustApproximate Some(dateTimeNow)
          theHead.registeredBy mustBe Some(dummyUser.id)
          theHead.registeredDate mustApproximate Some(dateTimeNow)
          theHead.objectId mustBe Some(parentId)

        case _ =>
          fail(s"The list contained ${sce.size} elements, expected 1.")
      }
    }

    "delete the sample by its uuid" in {
      val found = service.findById(addedId.value).futureValue.successValue.value
      val so    = service.delete(found.objectId.get).futureValue
      so mustBe MusitSuccess(())
    }
  }
}
