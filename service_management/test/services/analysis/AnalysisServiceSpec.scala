package services.analysis

import models.analysis.events.AnalysisResults.{DatingResult, GenericResult}
import models.analysis.events.{Analysis, AnalysisCollection}
import models.analysis.events.EventCategories.Genetic
import no.uio.musit.models.{ActorId, EventId}
import no.uio.musit.models.MuseumCollections.Archeology
import no.uio.musit.security.{AuthenticatedUser, SessionUUID, UserInfo, UserSession}
import no.uio.musit.time.dateTimeNow
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.{DateTimeMatchers, MusitResultValues}
import org.scalatest.Inspectors.forAll
import org.scalatest.OptionValues
import utils.{AnalysisGenerators, AnalysisValidators}

class AnalysisServiceSpec
    extends MusitSpecWithAppPerSuite
    with DateTimeMatchers
    with MusitResultValues
    with OptionValues
    with AnalysisGenerators
    with AnalysisValidators {

  private val defaultUserId = ActorId.generate()

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

  val service = fromInstanceCache[AnalysisService]

  "The AnalysisService" should {

    "return all known event types" in {
      val res = service.getAllTypes.futureValue.successValue
      res.size mustBe 107
    }

    "return all known event types for a given event category" in {
      val res = service.getTypesFor(Genetic).futureValue.successValue
      res.size mustBe 7
    }

    "return all known event types for a museum collection" in {
      val res = service.getTypesFor(Archeology.uuid).futureValue.successValue
      res.size mustBe 83
    }

    "successfully add a new Analysis" in {
      val cmd = dummySaveAnalysisCmd()
      service.add(cmd).futureValue.successValue mustBe EventId(1L)
    }

    "successfully add a new AnalysisCollection" in {
      val cmd = dummySaveAnalysisCollectionCmd(oids = Seq(oid1, oid2, oid3))
      service.add(cmd).futureValue.successValue mustBe EventId(2L)
    }

    "return an analysis by its EventId" in {
      val res = service.findById(EventId(1L)).futureValue.successValue.value

      res.analysisTypeId mustBe dummyAnalysisTypeId
      res.doneBy mustBe Some(dummyActorById)
      res.doneDate mustApproximate Some(dateTimeNow)
      res.note mustBe Some("This is from a SaveAnalysis command")
      res.objectId must not be empty
      res.administrator mustBe Some(dummyActorById)
      res.responsible mustBe Some(dummyActorById)
      res.completedBy mustBe empty
      res.completedDate mustBe empty
    }

    "return all child Analysis events for an AnalyisCollection" in {
      val res = service.childrenFor(EventId(2L)).futureValue.successValue

      res.size mustBe 3

      forAll(res) { r =>
        r.analysisTypeId mustBe dummyAnalysisTypeId
        r.doneBy mustBe Some(dummyActorById)
        r.doneDate mustApproximate Some(dateTimeNow)
        r.note mustBe Some("This is from a SaveAnalysisCollection command")
        r.objectId must not be empty
        r.administrator mustBe Some(dummyActorById)
        r.responsible mustBe Some(dummyActorById)
        r.completedBy mustBe empty
        r.completedDate mustBe empty
      }
    }

    "return all analysis events associated with the given ObjectUUID" in {
      val res = service.findByObject(oid1).futureValue.successValue

      res.size mustBe 2

      forAll(res) { r =>
        r.analysisTypeId mustBe dummyAnalysisTypeId
        r.doneBy mustBe Some(dummyActorById)
        r.doneDate mustApproximate Some(dateTimeNow)
        r.note must not be empty
        r.note.value must startWith("This is from a SaveAnalysis")
        r.objectId must not be empty
        r.administrator mustBe Some(dummyActorById)
        r.responsible mustBe Some(dummyActorById)
        r.completedBy mustBe empty
        r.completedDate mustBe empty
      }
    }

    "successfully add a result to an Analysis" in {
      val gr = dummyGenericResult(
        extRef = Some(Seq("foobar", "fizzbuzz")),
        comment = Some("This is a generic result")
      )

      service.addResult(EventId(1L), gr).futureValue.successValue mustBe EventId(1L)

      val ares = service.findById(EventId(1L)).futureValue.successValue.value

      ares match {
        case a: Analysis =>
          a.result must not be empty
          validateResult(a.result.value, gr, Some(defaultUserId), Some(dateTimeNow))

        case other =>
          fail(s"Expected an ${classOf[Analysis]} but got ${other.getClass}")
      }
    }

    "successfully add a result to an AnalysisCollection and its children" in {
      val dr = dummyDatingResult(
        extRef = Some(Seq("foobar", "fizzbuzz")),
        comment = Some("This is a generic result"),
        age = Some("really old")
      )

      service.addResult(EventId(2L), dr).futureValue.successValue mustBe EventId(2L)

      val ares = service.findById(EventId(2L)).futureValue.successValue.value

      ares match {
        case a: AnalysisCollection =>
          a.result must not be empty
          a.result.value match {
            case r: DatingResult =>
              validateResult(r, dr, Some(defaultUserId), Some(dateTimeNow))

            case boo =>
              fail(s"Expected a ${classOf[DatingResult]} but got ${boo.getClass}")
          }

          forAll(a.events)(_.result mustBe empty)

        case other =>
          fail(s"Expected an ${classOf[AnalysisCollection]} but got ${other.getClass}")
      }
    }

    "successfully update the result for an Analysis" in {
      val eid  = EventId(1L)
      val orig = service.findById(eid).futureValue.successValue.value
      orig mustBe an[Analysis]

      val origRes = orig.asInstanceOf[Analysis].result.value
      origRes mustBe a[GenericResult]

      val upd = origRes.asInstanceOf[GenericResult].copy(comment = Some("updated"))

      service.updateResult(eid, upd).futureValue.isSuccess mustBe true

      val updRes = service.findById(eid).futureValue.successValue.value
      updRes mustBe an[Analysis]
      updRes.asInstanceOf[Analysis].result.value match {
        case gr: GenericResult =>
          gr mustBe upd

        case err =>
          fail(s"Expected ${classOf[GenericResult]}, got ${err.getClass}")
      }
    }

    "successfully update the result for an AnalysisCollection" in {
      val eid  = EventId(2L)
      val orig = service.findById(eid).futureValue.successValue.value
      orig mustBe an[AnalysisCollection]

      val origRes = orig.asInstanceOf[AnalysisCollection].result.value
      origRes mustBe a[DatingResult]

      val upd = origRes.asInstanceOf[DatingResult].copy(comment = Some("updated"))

      service.updateResult(eid, upd).futureValue.isSuccess mustBe true

      val updRes = service.findById(eid).futureValue.successValue.value
      updRes mustBe an[AnalysisCollection]
      updRes.asInstanceOf[AnalysisCollection].result.value match {
        case gr: DatingResult =>
          gr mustBe upd

        case err =>
          fail(s"Expected ${classOf[GenericResult]}, got ${err.getClass}")
      }
    }

    "successfully update an Analysis" in {
      val expectedId = EventId(6L)

      val cmd = dummySaveAnalysisCmd()
      service.add(cmd).futureValue.successValue mustBe expectedId

      val updCmd = cmd.copy(note = Some("This is an updated note"))
      val res    = service.update(defaultMid, expectedId, updCmd).futureValue.successValue

      res must not be empty

      res.value match {
        case a: Analysis =>
          a.note mustBe updCmd.note
          a.updatedBy mustBe Some(defaultUserId)
          a.updatedDate mustApproximate Some(dateTimeNow)

        case other =>
          fail(s"Expected an ${classOf[Analysis]} but got ${other.getClass}")
      }
    }

    "successfully update an AnalysisCollection" in {
      val expectedId = EventId(7L)

      val cmd = dummySaveAnalysisCollectionCmd()
      service.add(cmd).futureValue.successValue mustBe expectedId

      val updCmd = cmd.copy(note = Some("This is an updated note"))
      val res    = service.update(defaultMid, expectedId, updCmd).futureValue.successValue

      res must not be empty

      res.value match {
        case a: AnalysisCollection =>
          a.note mustBe updCmd.note
          a.updatedBy mustBe Some(defaultUserId)
          a.updatedDate mustApproximate Some(dateTimeNow)

        case other =>
          fail(s"Expected an ${classOf[Analysis]} but got ${other.getClass}")
      }
    }

  }

}
