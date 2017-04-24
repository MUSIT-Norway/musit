package models.analysis

import models.analysis.events.AnalysisResults.GenericResult
import models.analysis.events._
import no.uio.musit.formatters.DateTimeFormatters.dateTimeFormatter
import no.uio.musit.models.{ActorId, EventId, ObjectUUID}
import no.uio.musit.test.matchers.DateTimeMatchers
import org.joda.time.DateTime
import org.scalatest.{Inside, MustMatchers, WordSpec}
import play.api.libs.json._

class AnalysisEventSpec
    extends WordSpec
    with MustMatchers
    with DateTimeMatchers
    with Inside {

  val dummyEventId        = EventId(1L)
  val dummyAnalysisTypeId = AnalysisTypeId.generate()
  val dummyDate           = DateTime.now
  val dummyActor          = ActorId.generate()
  val dummyObject         = ObjectUUID.generate()
  val dummyNote           = "Foo bar"
  val dummyRestriction = Restriction(
    requester = "holder",
    registeredStamp = None,
    expirationDate = DateTime.now.plusDays(50),
    reason = "reason",
    caseNumbers = None
  )

  def createAnalysis() =
    Analysis(
      id = Some(dummyEventId),
      analysisTypeId = dummyAnalysisTypeId,
      doneBy = Some(dummyActor),
      doneDate = Some(dummyDate),
      registeredBy = Some(dummyActor),
      registeredDate = Some(dummyDate),
      responsible = Some(dummyActor),
      administrator = Some(dummyActor),
      updatedBy = Some(dummyActor),
      updatedDate = Some(dummyDate),
      completedBy = Some(dummyActor),
      completedDate = Some(dummyDate),
      objectId = Some(dummyObject),
      partOf = None,
      note = Some(dummyNote),
      result = Some(
        GenericResult(
          registeredBy = Some(dummyActor),
          registeredDate = Some(dummyDate),
          extRef = Some(Seq(dummyNote)),
          comment = Some(dummyNote)
        )
      )
    )

  def createAnalysisCollection() =
    AnalysisCollection(
      id = Some(dummyEventId),
      analysisTypeId = dummyAnalysisTypeId,
      doneBy = Some(dummyActor),
      doneDate = Some(dummyDate),
      registeredBy = Some(dummyActor),
      registeredDate = Some(dummyDate),
      responsible = Some(dummyActor),
      administrator = Some(dummyActor),
      updatedBy = Some(dummyActor),
      updatedDate = Some(dummyDate),
      completedBy = Some(dummyActor),
      completedDate = Some(dummyDate),
      note = Some(dummyNote),
      restriction = Some(dummyRestriction),
      result = Some(
        GenericResult(
          registeredBy = Some(dummyActor),
          registeredDate = Some(dummyDate),
          extRef = Some(Seq(dummyNote)),
          comment = Some(dummyNote)
        )
      ),
      events = Seq(
        createAnalysis(),
        createAnalysis()
      )
    )

  "AnalysisEvent" should {

    "serialize an Analysis instance to JSON" in {
      val js = Json.toJson(createAnalysis())

      (js \ "type").as[String] mustBe Analysis.discriminator
      (js \ "id").as[Long] mustBe dummyEventId.underlying
      (js \ "analysisTypeId").as[String] mustBe dummyAnalysisTypeId.asString
      (js \ "doneDate").as[DateTime] mustApproximate dummyDate
      (js \ "registeredBy").as[String] mustBe dummyActor.asString
      (js \ "registeredDate").as[DateTime] mustApproximate dummyDate
      (js \ "partOf").asOpt[Long] mustBe None
      (js \ "note").as[String] mustBe dummyNote
      (js \ "result" \ "type").as[String] mustBe GenericResult.resultType
      (js \ "result" \ "registeredBy").as[String] mustBe dummyActor.asString
      (js \ "result" \ "registeredDate").as[DateTime] mustApproximate dummyDate
      (js \ "result" \ "extRef" \ 0).as[String] mustBe dummyNote
      (js \ "result" \ "comment").as[String] mustBe dummyNote
    }

    "serialize an AnalysisCollection instance to JSON" in {
      val js = Json.toJson(createAnalysisCollection())

      (js \ "type").as[String] mustBe AnalysisCollection.discriminator
      (js \ "id").as[Long] mustBe dummyEventId.underlying
      (js \ "analysisTypeId").as[String] mustBe dummyAnalysisTypeId.asString
      (js \ "doneBy").as[String] mustBe dummyActor.asString
      (js \ "doneDate").as[DateTime] mustApproximate dummyDate
      (js \ "registeredBy").as[String] mustBe dummyActor.asString
      (js \ "registeredDate").as[DateTime] mustApproximate dummyDate
      (js \ "responsible").as[String] mustBe dummyActor.asString
      (js \ "administrator").as[String] mustBe dummyActor.asString
      (js \ "updatedBy").as[String] mustBe dummyActor.asString
      (js \ "updatedDate").as[DateTime] mustApproximate dummyDate
      (js \ "completedBy").as[String] mustBe dummyActor.asString
      (js \ "completedDate").as[DateTime] mustApproximate dummyDate
      (js \ "note").as[String] mustBe dummyNote
      (js \ "result" \ "type").as[String] mustBe GenericResult.resultType
      (js \ "result" \ "registeredBy").as[String] mustBe dummyActor.asString
      (js \ "result" \ "registeredDate").as[DateTime] mustApproximate dummyDate
      (js \ "result" \ "extRef" \ 0).as[String] mustBe dummyNote
      (js \ "result" \ "comment").as[String] mustBe dummyNote
      (js \ "events").as[JsArray].value.size mustBe 2
      (js \ "events" \ 0 \ "type").as[String] mustBe Analysis.discriminator
      (js \ "events" \ 1 \ "type").as[String] mustBe Analysis.discriminator
    }

    "fail deserializing an Analysis if the discriminator isn't present" in {
      val js = Json.toJson(createAnalysis())

      val transform = js.transform((__ \ "type").json.prune)
      inside(transform) {
        case JsSuccess(noTypeJs, _) =>
          Json.fromJson[AnalysisEvent](noTypeJs) match {
            case JsSuccess(_, _) => fail("Expected deserialization to fail.")
            case JsError(errors) => errors.size mustBe 1
          }
      }
    }

    "fail deserializing an AnalysisCollection if the discriminator isn't present" in {
      val js = Json.toJson(createAnalysisCollection())

      val transform = js.transform((__ \ "type").json.prune)
      inside(transform) {
        case JsSuccess(noTypeJs, _) =>
          Json.fromJson[AnalysisEvent](noTypeJs) match {
            case JsSuccess(_, _) => fail("Expected deserialization to fail.")
            case JsError(errors) => errors.size mustBe 1
          }
      }
    }

  }

}
