package services.analysis

import models.analysis.events.AnalysisExtras.{MicroscopyAttributes, TomographyAttributes}
import models.analysis.events.AnalysisResults.{AgeResult, GenericResult}
import models.analysis.events.EventCategories.{Genetic, Image}
import models.analysis.events._
import no.uio.musit.models.MuseumCollections.Archeology
import no.uio.musit.models.{EventId, ObjectUUID, OrgId}
import no.uio.musit.security.{AuthenticatedUser, SessionUUID, UserInfo, UserSession}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.{DateTimeMatchers, MusitResultValues}
import no.uio.musit.time.dateTimeNow
import org.scalatest.Inside.inside
import org.scalatest.Inspectors.forAll
import org.scalatest.OptionValues
import utils.testdata.AnalysisGenerators
import utils.validators.AnalysisValidators

class AnalysisServiceSpec
    extends MusitSpecWithAppPerSuite
    with DateTimeMatchers
    with MusitResultValues
    with OptionValues
    with AnalysisGenerators
    with AnalysisValidators {

  implicit val dummyUser = AuthenticatedUser(
    session = UserSession(uuid = SessionUUID.generate()),
    userInfo = UserInfo(
      id = dummyActorId,
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
      res.size mustBe 46
    }

    "return all known event types for an event category" in {
      val res = service.getTypesFor(Some(Genetic), None).futureValue.successValue
      res.size mustBe 6
    }

    "return event types with extra description attributes for a category" in {
      val res = service.getTypesFor(Some(Image), None).futureValue.successValue
      res.size mustBe 4

      forAll(res) { at =>
        at.category mustBe Image
      }

      val withExtras = res.filter(_.extraDescriptionAttributes.isDefined)

      withExtras.size mustBe 2
      withExtras.flatMap(_.extraDescriptionType) must contain allOf (
        MicroscopyAttributes.typeName,
        TomographyAttributes.typeName
      )

      // lift out the allowed values to ensure the valid ones are present for
      // each analysis type.
      val enrichedAttributes = withExtras
        .flatMap(_.extraDescriptionAttributes)
        .flatMap(_.flatMap(_.allowedValues))

      enrichedAttributes must contain allOf (
        MicroscopyAttributes.allValues,
        TomographyAttributes.allValues
      )
    }

    "return all known event types for a museum collection" ignore {
      val res = service.getTypesFor(None, Some(Archeology.uuid)).futureValue.successValue
      res.size mustBe 83
    }

    "successfully add a new Analysis" in {
      val cmd = dummySaveAnalysisCmd()
      service.add(defaultMid, cmd).futureValue.successValue match {
        case Some(analysis: Analysis) =>
          verifyBasicAnalysisFields(analysis, Some(EventId(1L)), Some(dummyAnalysisNote))
          analysis.objectId must not be empty
        case wrong =>
          fail(s"Expected ${Analysis.getClass}, but got $wrong")
      }
    }

    "successfully add a new AnalysisCollection" in {
      val cmd = dummySaveAnalysisCollectionCmd(oids = Seq(oid1, oid2, oid3))
      service.add(defaultMid, cmd).futureValue.successValue match {
        case Some(analysis: AnalysisCollection) =>
          verifyBasicAnalysisFields(
            analysis,
            Some(EventId(2L)),
            Some(dummyAnalysisCollectionNote)
          )
          analysis.events must not be empty
        case wrong =>
          fail(s"Expected ${AnalysisCollection.getClass}, but got $wrong")
      }
    }

    "return an analysis by its EventId" in {
      val expectedId = EventId(1L)
      service.findById(defaultMid, expectedId).futureValue.successValue.value match {
        case res: AnalysisEvent =>
          verifyBasicAnalysisFields(res, Some(expectedId), Some(dummyAnalysisNote))
          res.objectId must not be empty

        case wrong =>
          fail(s"Expected an ${AnalysisEvent.getClass}, but got ${wrong.getClass}")
      }
    }

    "return all child Analysis events for an AnalyisCollection" in {
      val expectedId = EventId(2L)
      val res        = service.childrenFor(defaultMid, expectedId).futureValue.successValue

      res.size mustBe 3

      forAll(res) { r =>
        verifyBasicAnalysisFields(
          r,
          None,
          Some("This is from a SaveAnalysisCollection command")
        )
      }
    }

    "return all analysis collection events associated with the given ObjectUUID" in {
      val res = service.findByObject(defaultMid, oid1).futureValue.successValue

      res.size mustBe 1

      forAll(res) { r =>
        r.analysisTypeId mustBe dummyAnalysisTypeId
        r.doneBy mustBe Some(dummyActorId)
        r.doneDate mustApproximate Some(dateTimeNow)
        r.note must not be empty
        r.note.value must startWith("This is from a SaveAnalysis")
        r.objectId mustBe empty
      }
    }

    "successfully add a result to an Analysis" in {
      val gr = dummyGenericResult(
        extRef = Some(Seq("foobar", "fizzbuzz")),
        comment = Some("This is a generic result")
      )

      service
        .addResult(defaultMid, EventId(1L), gr)
        .futureValue
        .successValue mustBe EventId(1L)

      val ares = service.findById(defaultMid, EventId(1L)).futureValue.successValue.value

      ares match {
        case a: Analysis =>
          a.result must not be empty
          validateResult(a.result.value, gr, Some(dummyActorId), Some(dateTimeNow))

        case other =>
          fail(s"Expected an ${classOf[Analysis]} but got ${other.getClass}")
      }
    }

    "successfully add a result to an AnalysisCollection and its children" in {
      val dr = dummyAgeResult(
        extRef = Some(Seq("foobar", "fizzbuzz")),
        comment = Some("This is a generic result"),
        age = Some("really old")
      )

      service
        .addResult(defaultMid, EventId(2L), dr)
        .futureValue
        .successValue mustBe EventId(2L)

      val ares = service.findById(defaultMid, EventId(2L)).futureValue.successValue.value

      ares match {
        case a: AnalysisCollection =>
          a.result must not be empty
          a.result.value match {
            case r: AgeResult =>
              validateResult(r, dr, Some(dummyActorId), Some(dateTimeNow))

            case boo =>
              fail(s"Expected a ${classOf[AgeResult]} but got ${boo.getClass}")
          }

          forAll(a.events)(_.result mustBe empty)

        case other =>
          fail(s"Expected an ${classOf[AnalysisCollection]} but got ${other.getClass}")
      }
    }

    "successfully update the result for an Analysis" in {
      val eid  = EventId(1L)
      val orig = service.findById(defaultMid, eid).futureValue.successValue.value
      orig mustBe an[Analysis]

      val origRes = orig.asInstanceOf[Analysis].result.value
      origRes mustBe a[GenericResult]

      val upd = origRes.asInstanceOf[GenericResult].copy(comment = Some("updated"))

      service.updateResult(defaultMid, eid, upd).futureValue.isSuccess mustBe true

      val updRes = service.findById(defaultMid, eid).futureValue.successValue.value
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
      val orig = service.findById(defaultMid, eid).futureValue.successValue.value
      orig mustBe an[AnalysisCollection]

      val origRes = orig.asInstanceOf[AnalysisCollection].result.value
      origRes mustBe a[AgeResult]

      val upd = origRes.asInstanceOf[AgeResult].copy(comment = Some("updated"))

      service.updateResult(defaultMid, eid, upd).futureValue.isSuccess mustBe true

      val updRes = service.findById(defaultMid, eid).futureValue.successValue.value
      updRes mustBe an[AnalysisCollection]
      updRes.asInstanceOf[AnalysisCollection].result.value match {
        case gr: AgeResult =>
          gr mustBe upd

        case err =>
          fail(s"Expected ${classOf[GenericResult]}, got ${err.getClass}")
      }
    }

    "successfully update an Analysis" in {
      val expectedId = EventId(6L)

      val cmd = dummySaveAnalysisCmd()
      service.add(defaultMid, cmd).futureValue.successValue match {
        case Some(analysis: Analysis) =>
          verifyBasicAnalysisFields(analysis, Some(expectedId), Some(dummyAnalysisNote))
        case wrong =>
          fail(s"Expected ${Analysis.getClass}, but got $wrong")
      }

      val updCmd = cmd.copy(note = Some("This is an updated note"))
      val res    = service.update(defaultMid, expectedId, updCmd).futureValue.successValue

      res must not be empty

      res.value match {
        case a: Analysis =>
          a.note mustBe updCmd.note
          a.updatedBy mustBe Some(dummyActorId)
          a.updatedDate mustApproximate Some(dateTimeNow)

        case other =>
          fail(s"Expected an ${classOf[Analysis]} but got ${other.getClass}")
      }
    }

    "successfully update an AnalysisCollection" in {
      val expectedId = EventId(7L)

      val cmd = dummySaveAnalysisCollectionCmd()
      service.add(defaultMid, cmd).futureValue.successValue match {
        case Some(analysis: AnalysisCollection) =>
          verifyBasicAnalysisFields(
            analysis,
            Some(expectedId),
            Some(dummyAnalysisCollectionNote)
          )
          analysis.events must not be empty
        case wrong =>
          fail(s"Expected ${AnalysisCollection.getClass}, but got $wrong")
      }

      val updCmd =
        cmd.copy(note = Some("This is an updated note"), orgId = Some(OrgId(316)))
      val res = service.update(defaultMid, expectedId, updCmd).futureValue.successValue

      res must not be empty

      res.value match {
        case a: AnalysisCollection =>
          a.note mustBe updCmd.note
          a.updatedBy mustBe Some(dummyActorId)
          a.updatedDate mustApproximate Some(dateTimeNow)
          a.orgId.get.underlying mustBe 316

        case other =>
          fail(s"Expected an ${classOf[Analysis]} but got ${other.getClass}")
      }
    }

    "successfully add results to an AnalysisCollection and its children" in {
      val oids = Seq(
        "376d41e7-c463-45e8-9bde-7a2c9844637e",
        "2350578d-0bb0-4601-92d4-817478ad0952",
        "c182206b-530c-4a40-b9aa-fba044ecb953",
        "bf53f481-1db3-4474-98ee-c94df31ec251",
        "373bb138-ed93-472b-ad57-ccb77ab8c151",
        "62272640-e29e-4af4-a537-3c49b5f1cf42",
        "f4a189c3-4d8f-4258-9000-b23282814278",
        "67965e71-27ee-4ef0-ad66-e7e321882f33"
      ).map(ObjectUUID.unsafeFromString)

      val cmd = dummySaveAnalysisCollectionCmd(oids, None)

      val ac = service.add(defaultMid, cmd).futureValue.successValue.value
      ac.id must not be empty
      ac mustBe an[AnalysisCollection]
      val eventId        = ac.id.value
      val analysisEvents = ac.asInstanceOf[AnalysisCollection].events

      val colResult = dummyAgeResult(
        extRef = None,
        comment = Some("Collection Result"),
        age = None
      )
      val childResults =
        analysisEvents.zipWithIndex.map {
          case (analysis, idx) =>
            ResultForObjectEvent(
              analysis.objectId.value,
              analysis.id.value,
              dummyAgeResult(
                extRef = None,
                comment = Some(s"res for ${analysis.id.value}"),
                age = Some(s"$idx years")
              )
            )
        }

      val resultImport = AnalysisResultImport(colResult, childResults)

      service
        .updateResults(defaultMid, eventId, resultImport)
        .futureValue
        .isSuccess mustBe true

      // verify results
      service.findById(defaultMid, eventId).futureValue.successValue.value match {
        case ac: AnalysisCollection =>
          ac.id mustBe Some(eventId)
          inside(ac.result.value) {
            case AgeResult(registeredBy, _, extRef, comment, age) =>
              registeredBy mustBe Some(dummyActorId)
              extRef mustBe None
              comment mustBe Some("Collection Result")
              age mustBe None

            case e2 =>
              fail(s"Expected an AgeResult but got ${e2.getClass}")
          }
          ac.events.size mustBe 8

          val results = ac.events.flatMap(_.result)
          results.size mustBe 8
          forAll(results) {
            case AgeResult(registeredBy, _, extRef, comment, age) =>
              registeredBy mustBe Some(dummyActorId)
              extRef mustBe None
              comment.value must startWith("res for ")
              age.value must endWith(" years")

            case e2 =>
              fail(s"Expected an AgeResult but got ${e2.getClass}")
          }

        case e1 =>
          fail(s"Expected an AnalysisCollection but got ${e1.getClass}")
      }
    }

  }

  /**
   * verifies basic fields on an analysis
   *
   * @param res          the analysis event
   * @param expectedId   if not provided, not checked
   * @param expectedNote if not provided, not checked
   * @return
   */
  private def verifyBasicAnalysisFields(
      res: AnalysisEvent,
      expectedId: Option[EventId],
      expectedNote: Option[String]
  ) = {
    if (expectedId.isDefined) {
      res.id mustBe expectedId
    } else {
      res.id must not be empty
    }
    if (expectedNote.isDefined) {
      res.note mustBe expectedNote
    } else {
      res.note must not be empty
    }
    res.analysisTypeId mustBe dummyAnalysisTypeId
    res.doneBy mustBe Some(dummyActorId)
    res.doneDate mustApproximate Some(dateTimeNow)
    res.administrator mustBe Some(dummyActorById)
    res.responsible mustBe Some(dummyActorById)
    res.completedBy mustBe empty
    res.completedDate mustBe empty
  }
}
