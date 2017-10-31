package controllers.conservation

import models.conservation.events.ConservationProcess
import no.uio.musit.models.{ActorId, ObjectUUID}
import no.uio.musit.test.matchers.DateTimeMatchers
import no.uio.musit.test.{FakeUsers, MusitSpec}
import no.uio.musit.time.dateTimeNow
import no.uio.musit.formatters.DateTimeFormatters.dateTimeFormatter
import org.joda.time.DateTime
import play.api.libs.json._

trait ConservationJsonGenerators { // test data
  val conservationProcessEventTypeId  = 1
  val treatmentEventTypeId            = 2
  val technicalDescriptionEventTypeId = 3

  val validObjectUUIDs = Seq(
    ObjectUUID.unsafeFromString("baab2f60-4f49-40fe-99c8-174b13b12d46"),
    ObjectUUID.unsafeFromString("376d41e7-c463-45e8-9bde-7a2c9844637e"),
    ObjectUUID.unsafeFromString("2350578d-0bb0-4601-92d4-817478ad0952"),
    ObjectUUID.unsafeFromString("c182206b-530c-4a40-b9aa-fba044ecb953"),
    ObjectUUID.unsafeFromString("bf53f481-1db3-4474-98ee-c94df31ec251"),
    ObjectUUID.unsafeFromString("373bb138-ed93-472b-ad57-ccb77ab8c151"),
    ObjectUUID.unsafeFromString("62272640-e29e-4af4-a537-3c49b5f1cf42"),
    ObjectUUID.unsafeFromString("f4a189c3-4d8f-4258-9000-b23282814278"),
    ObjectUUID.unsafeFromString("67965e71-27ee-4ef0-ad66-e7e321882f33"),
    ObjectUUID.unsafeFromString("6f9db6a5-f994-4498-8ebc-c9ba75c51ce8"),
    ObjectUUID.unsafeFromString("b17b8735-2350-4de9-b812-93753b1eeb8d"),
    ObjectUUID.unsafeFromString("215542d3-48c9-44af-a6ea-4c494da54fe0"),
    ObjectUUID.unsafeFromString("065b9812-0f22-4ba4-bac2-7d7cbc850dcc"),
    ObjectUUID.unsafeFromString("6ca2bf73-fa17-4d41-a8da-ab9f46a7494b"),
    ObjectUUID.unsafeFromString("21dadc0d-50ca-41ea-9b48-90fdec515148"),
    ObjectUUID.unsafeFromString("7b2e3bd6-b699-4671-bd50-1d964342f531"),
    ObjectUUID.unsafeFromString("40898454-069e-41c9-9551-946a1e693f59"),
    ObjectUUID.unsafeFromString("f56bd93f-49b6-4111-b6c8-bd84a14ea98e"),
    ObjectUUID.unsafeFromString("b71d3bd7-28e3-4790-b4f6-98664e2385ba"),
    ObjectUUID.unsafeFromString("6ee89241-a404-4086-b373-c42dd0a2e56a"),
    ObjectUUID.unsafeFromString("5f7f0b2b-f2eb-480d-816e-00b140705f2b"),
    ObjectUUID.unsafeFromString("bbf9a3de-9203-4e90-9b04-4475e4f7f749"),
    ObjectUUID.unsafeFromString("29339044-8696-4c76-9b3e-f153ae63d262"),
    ObjectUUID.unsafeFromString("7ae2521e-904c-432b-998c-bb09810310a9"),
    ObjectUUID.unsafeFromString("42b6a92e-de59-4fde-9c46-5c8794be0b34")
  )

  val testObjectUUID     = validObjectUUIDs.head
  val testObjectUUIDTail = validObjectUUIDs.tail.head

  val testAffectedThings = Seq(testObjectUUID, testObjectUUIDTail)

  val adminId    = ActorId.unsafeFromString(FakeUsers.testAdminId)
  val testUserId = ActorId.unsafeFromString(FakeUsers.testUserId)

  def dummyEventJSON(
      typeId: Int,
      doneDate: Option[DateTime],
      note: Option[String],
      caseNumber: Option[String],
      affectedThings: Option[Seq[ObjectUUID]]
  ): JsObject = {
    val js1 = Json.obj(
      "eventTypeId"  -> typeId,
      "doneBy"       -> adminId,
      "registeredBy" -> adminId,
      "updatedBy"    -> adminId,
      "completedBy"  -> adminId
    )
    val js2 = note.map(n => js1 ++ Json.obj("note" -> n)).getOrElse(js1)
    val js3 = doneDate.map { d =>
      js2 ++ Json.obj("doneDate" -> Json.toJson[DateTime](d))
    }.getOrElse(js2)
    val js4 =
      js3 ++ Json.obj("registeredDate" -> Json.toJson[DateTime](dateTimeNow))

    val js5 =
      js4 ++ Json.obj("updatedDate" -> Json.toJson[DateTime](dateTimeNow))

    val js6 =
      js5 ++ Json.obj("caseNumber" -> Json.toJson(caseNumber))
    val js7 = affectedThings
      .map(
        o =>
          js6 ++ Json.obj(
            "affectedThings" ->
              JsArray(o.map(m => JsString(m.underlying.toString)))
        )
      )
      .getOrElse(js6)
    js7
  }
}

trait ConservationJsonValidators {
  self: MusitSpec with DateTimeMatchers with ConservationJsonGenerators =>

  def validateConservationType(
      expectedName: String,
      expectedShortName: Option[String],
      expectedExtraAttributes: Option[Map[String, String]],
      actual: JsValue
  ) = {
    (actual \ "id").asOpt[String] must not be empty
    (actual \ "name").as[String] mustBe expectedName
    (actual \ "shortName").asOpt[String] mustBe expectedShortName
    (actual \ "extraAttributes").asOpt[Map[String, String]] mustBe expectedName
  }

  /*def validateConservationProcess(
      expectedId: Long,
      expectedTypeId: Int,
      expectedDoneDate: Option[DateTime],
      expectedNote: Option[String],
      expectedThings: Option[Seq[ObjectUUID]],
      actual: JsValue
  ) = {
    (actual \ "type").as[String] mustBe ConservationProcess.discriminator
    (actual \ "id").as[Long] mustBe expectedId
    (actual \ "eventTypeId").as[Int] mustBe expectedTypeId
    (actual \ "doneDate").asOpt[DateTime] mustApproximate expectedDoneDate
    (actual \ "affectedThings").asOpt[Seq[String]] mustBe expectedThings.map(
      _.map(m => m.asString)
    )
    (actual \ "note").asOpt[String] mustBe expectedNote
    (actual \ "registeredBy").asOpt[String] must not be empty
    (actual \ "registeredDate").asOpt[DateTime] must not be empty
  }*/
}
