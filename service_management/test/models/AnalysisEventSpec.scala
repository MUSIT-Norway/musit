package models

import models.events.AnalysisResults.GenericResult
import models.events._
import no.uio.musit.formatters.DateTimeFormatters.dateTimeFormatter
import no.uio.musit.models.{ActorId, EventId, ObjectUUID}
import no.uio.musit.test.matchers.DateTimeMatchers
import org.joda.time.DateTime
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json._

class AnalysisEventSpec extends WordSpec
    with MustMatchers
    with DateTimeMatchers {

  val dummyEventId = EventId(1L)
  val dummyAnalysisTypeId = AnalysisTypeId.generate()
  val dummyDate = DateTime.now
  val dummyActor = ActorId.generate()
  val dummyObject = ObjectUUID.generate()
  val dummyNote = "Foo bar"

  def createAnalysis() =
    Analysis(
      id = Some(dummyEventId),
      analysisTypeId = dummyAnalysisTypeId,
      eventDate = Some(dummyDate),
      registeredBy = Some(dummyActor),
      registeredDate = Some(dummyDate),
      objectId = Some(dummyObject),
      partOf = None,
      note = Some(dummyNote),
      result = Some(GenericResult(
        registeredBy = Some(dummyActor),
        registeredDate = Some(dummyDate),
        extRef = Some(Seq(dummyNote)),
        comment = Some(dummyNote)
      ))
    )

  def createAnalysisCollection() =
    AnalysisCollection(
      id = Some(dummyEventId),
      analysisTypeId = dummyAnalysisTypeId,
      eventDate = Some(dummyDate),
      registeredBy = Some(dummyActor),
      registeredDate = Some(dummyDate),
      events = Seq(
        createAnalysis(),
        createAnalysis()
      )
    )

  "AnalysisEvent" should {

    "serialize an Analysis instance to JSON" in {
      val a = createAnalysis()

      val js = Json.toJson(a)

      (js \ "type").as[String] mustBe Analysis.discriminator
      (js \ "id").as[Long] mustBe dummyEventId.underlying
      (js \ "analysisTypeId").as[String] mustBe dummyAnalysisTypeId.asString
      (js \ "eventDate").as[DateTime] mustApproximate dummyDate
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
      val ac = createAnalysisCollection()

      val js = Json.toJson(ac)

      (js \ "type").as[String] mustBe AnalysisCollection.discriminator
      (js \ "id").as[Long] mustBe dummyEventId.underlying
      (js \ "eventDate").as[DateTime] mustApproximate dummyDate
      (js \ "registeredBy").as[String] mustBe dummyActor.asString
      (js \ "registeredDate").as[DateTime] mustApproximate dummyDate
      (js \ "events").as[JsArray].value.size mustBe 2
      (js \ "events" \ 0 \ "type").as[String] mustBe Analysis.discriminator
      (js \ "events" \ 1 \ "type").as[String] mustBe Analysis.discriminator
    }

    "fail deserializing an Analysis if the discriminator isn't present" in {
      val a = createAnalysis()
      val js = Json.toJson(a)

      val noTypeJs = js.transform((__ \ "type").json.prune).get

      Json.fromJson[AnalysisEvent](noTypeJs) match {
        case JsSuccess(_, _) => fail("Expected deserialization to fail.")
        case JsError(errors) => errors.size mustBe 1
      }
    }

    "fail deserializing an AnalysisCollection if the discriminator isn't present" in {
      val a = createAnalysisCollection()
      val js = Json.toJson(a)

      val noTypeJs = js.transform((__ \ "type").json.prune).get

      Json.fromJson[AnalysisEvent](noTypeJs) match {
        case JsSuccess(_, _) => fail("Expected deserialization to fail.")
        case JsError(errors) => errors.size mustBe 1
      }
    }

  }

}
