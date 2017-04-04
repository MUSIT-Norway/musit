package services.analysis

import models.analysis.events.SampleCreated
import models.analysis.{SampleObject, SampleStatuses}
import no.uio.musit.models.ObjectTypes.{CollectionObject, ObjectType}
import no.uio.musit.models.{ActorId, Museums, ObjectUUID}
import no.uio.musit.security.{AuthenticatedUser, SessionUUID, UserInfo, UserSession}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.{DateTimeMatchers, MusitResultValues}
import no.uio.musit.time

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

  val service      = fromInstanceCache[SampleObjectService]
  val eventService = fromInstanceCache[AnalysisService]

  def generateSampleObject(
      id: Option[ObjectUUID],
      parentId: Option[ObjectUUID],
      parentobjType: ObjectType = CollectionObject,
      isExtracted: Boolean = false
  ): SampleObject = {
    val now = time.dateTimeNow
    SampleObject(
      objectId = id,
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

  "The SampleObjectService " should {
    "returns the sampleObject that is inserted" in {
      val so =
        generateSampleObject(
          id = None,
          parentId = ObjectUUID.generateAsOpt(),
          isExtracted = true
        )
      val res  = service.add(so).futureValue
      val find = service.findById(res.get.underlying).futureValue
      find.successValue.value.isExtracted mustBe true
      val oid   = find.successValue.value.parentObjectId
      val event = eventService.findByObject(oid.value).futureValue.successValue
      event.size mustBe 1

      event.toList match {
        case theHead :: Nil =>
          theHead.analysisTypeId mustBe SampleCreated.sampleEventTypeId
          theHead.registeredBy mustBe find.successValue.value.registeredBy
          theHead.registeredDate mustApproximate find.successValue.value.registeredDate
          theHead.eventDate mustApproximate find.successValue.value.registeredDate
          theHead.objectId mustBe find.successValue.value.parentObjectId

        case _ =>
          fail(s"The list contained ${event.size} elements, expected 1.")
      }
    }

  }
}
