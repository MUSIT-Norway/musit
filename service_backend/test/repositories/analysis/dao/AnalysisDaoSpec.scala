package repositories.analysis.dao

import java.util.UUID

import models.analysis.ParentObject
import models.analysis.events.AnalysisResults.{AgeResult, AnalysisResult}
import models.analysis.events.{Analysis, AnalysisCollection}
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import no.uio.musit.models.ObjectTypes.{CollectionObjectType, SampleObjectType}
import no.uio.musit.models._
import no.uio.musit.security._
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.{DateTimeMatchers, MusitResultValues}
import no.uio.musit.time.dateTimeNow
import org.scalatest.Inspectors.forAll
import org.scalatest.OptionValues
import utils.testdata.{AnalysisGenerators, SampleObjectGenerators}

class AnalysisDaoSpec
    extends MusitSpecWithAppPerSuite
    with DateTimeMatchers
    with MusitResultValues
    with OptionValues
    with AnalysisGenerators
    with SampleObjectGenerators {

  private val sampleDao = fromInstanceCache[SampleObjectDao]
  private val dao       = fromInstanceCache[AnalysisDao]

  val collections = Seq(
    MuseumCollection(
      uuid = MuseumCollections.Archeology.uuid,
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
        module = CollectionManagement,
        permission = Permissions.Admin,
        museumId = mid,
        description = None,
        collections = collections
      )
    )
  )

  def saveSamplesFor(
      origObjectId: ObjectUUID,
      parentObject: ParentObject,
      mid: MuseumId = defaultMid
  ) = {
    val sid = ObjectUUID.generate()
    val s1 =
      generateSample(
        sid,
        parentObject.objectId,
        parentObject.objectType,
        origObjectId,
        mid = mid
      )
    sampleDao.insert(s1).futureValue.successValue
  }

  def saveAnalysis(
      oid: Option[ObjectUUID],
      res: Option[AnalysisResult],
      mid: MuseumId = defaultMid
  ): MusitResult[EventId] = {
    val a = dummyAnalysis(oid, res)
    dao.insert(mid, a).futureValue
  }

  def saveAnalysisCol(
      oid: Option[ObjectUUID],
      res: Option[AnalysisResult],
      mid: MuseumId = defaultMid
  ): MusitResult[EventId] = {
    val a = dummyAnalysisCollection(res, dummyAnalysis(oid, None))
    dao.insertCol(mid, a).futureValue
  }

  "AnalysisDao" when {

    "inserting analysis events" should {
      "return the EventId allocated to a single analysis" in {
        val gr = Some(dummyGenericResult())
        saveAnalysis(Some(oid1), gr) mustBe MusitSuccess(EventId(1))
      }

      "return the EventId allocated for an analysis collection with many analyses" in {
        val gr = Some(dummyGenericResult())
        val e1 = dummyAnalysis(Some(oid1))
        val e2 = dummyAnalysis(Some(oid2))
        val e3 = dummyAnalysis(Some(oid3))
        val ac = dummyAnalysisCollection(gr, e1, e2, e3)

        val res = dao.insertCol(defaultMid, ac).futureValue

        res.successValue mustBe EventId(2)
      }

      "return the EventId allocated for an analysis collection with one analysis" in {
        val e1 = dummyAnalysis(Some(oid1))
        val ac = dummyAnalysisCollection(None, e1)

        val res = dao.insertCol(defaultMid, ac).futureValue

        res.successValue mustBe EventId(6)

        val orgidRes = dao.findById(defaultMid, EventId(6)).futureValue
        orgidRes.successValue.value mustBe an[AnalysisCollection]
        val or = orgidRes.successValue.value.asInstanceOf[AnalysisCollection]
        or.orgId mustBe Some(OrgId(315))
      }
    }

    "fetching analysis events" should {

      "return an analysis collection with many children" in {
        val res = dao.findById(defaultMid, EventId(2)).futureValue

        res.successValue.value mustBe an[AnalysisCollection]
        val ac = res.successValue.value.asInstanceOf[AnalysisCollection]
        ac.events must not be empty
        ac.events.size mustBe 3
        ac.analysisTypeId mustBe dummyAnalysisTypeId
        ac.partOf mustBe empty
      }

      "return an analysis collection with one child" in {
        val res = dao.findById(defaultMid, EventId(6)).futureValue

        res.successValue.value mustBe an[AnalysisCollection]
        val ac = res.successValue.value.asInstanceOf[AnalysisCollection]
        ac.events must not be empty
        ac.events.size mustBe 1
        ac.analysisTypeId mustBe dummyAnalysisTypeId
        ac.partOf mustBe empty
      }

      "return each child in a collection separately by fetching them with their IDs" in {
        val res1 = dao.findById(defaultMid, EventId(3)).futureValue
        val res2 = dao.findById(defaultMid, EventId(4)).futureValue
        val res3 = dao.findById(defaultMid, EventId(5)).futureValue

        res1.successValue.value.partOf mustBe Some(EventId(2))
        res2.successValue.value.partOf mustBe Some(EventId(2))
        res3.successValue.value.partOf mustBe Some(EventId(2))
      }

      "list all child events for an analysis collection" in {
        val children = dao.listChildren(defaultMid, EventId(2)).futureValue
        children.successValue.size mustBe 3

        forAll(children.successValue) { child =>
          child.result mustBe None
          child.analysisTypeId mustBe dummyAnalysisTypeId
          child.partOf mustBe Some(EventId(2))
          child.registeredBy mustBe Some(dummyActorId)
        }
      }

      "return the analysis event that matches the given id" in {
        val gr =
          dummyGenericResult(extRef = Some(Seq("shizzle", "in", "the", "drizzle")))

        val mra = saveAnalysis(Some(oid2), Some(gr))

        val res = dao.findById(defaultMid, mra.successValue).futureValue
        res.successValue.value mustBe an[Analysis]
        val a = res.successValue.value.asInstanceOf[Analysis]
        a.registeredBy mustBe Some(dummyActorId)
        a.analysisTypeId mustBe dummyAnalysisTypeId
        a.result.value.comment mustBe gr.comment
        a.result.value.extRef mustBe gr.extRef
        a.result.value.registeredBy mustBe gr.registeredBy
      }

      "return an analysis event with a dating result" in {
        val dr = dummyAgeResult(age = Some("really really old"))

        val mra = saveAnalysis(Some(oid2), Some(dr))

        val res = dao.findById(defaultMid, mra.successValue).futureValue
        res.successValue.value mustBe an[Analysis]
        val analysis = res.successValue.value.asInstanceOf[Analysis]
        analysis.registeredBy mustBe Some(dummyActorId)
        analysis.analysisTypeId mustBe dummyAnalysisTypeId
        analysis.result.value mustBe a[AgeResult]
        val datingResult = analysis.result.value.asInstanceOf[AgeResult]
        datingResult.comment mustBe dr.comment
        datingResult.extRef mustBe dr.extRef
        datingResult.registeredBy mustBe dr.registeredBy
        datingResult.age mustBe dr.age
      }

      "return all analysis events for a given object" in {
        val s1 = saveSamplesFor(oid2, ParentObject(Some(oid2), CollectionObjectType))
        val s2 = saveSamplesFor(oid2, ParentObject(Some(s1), SampleObjectType))
        val s3 = saveSamplesFor(oid2, ParentObject(Some(s2), SampleObjectType))

        val r1 = dummyAgeResult(age = Some("Golden oldie"))
        val r2 = dummyGenericResult(comment = Some("Result 2"))

        val a1 = saveAnalysisCol(Some(s1), Some(r1))
        val a2 = saveAnalysisCol(Some(s2), Some(r2))
        val a3 = saveAnalysisCol(Some(s3), None)

        dao
          .findByCollectionObjectUUID(defaultMid, oid2)
          .futureValue
          .successValue
          .size mustBe 4
      }

      "successfully add a result to an analysis" in {
        val e1 = dummyAnalysis(Some(oid1))
        val ac = dummyAnalysisCollection(None, e1)
        val gr = dummyGenericResult(comment = Some("updated result"))

        val eid = dao.insertCol(defaultMid, ac).futureValue.successValue

        dao.upsertResult(defaultMid, eid, gr).futureValue.isSuccess mustBe true

        dao.findById(defaultMid, eid).futureValue.successValue.value match {
          case good: AnalysisCollection =>
            val res = good.result.value
            res.registeredBy mustBe gr.registeredBy
            res.registeredDate mustApproximate gr.registeredDate
            res.extRef mustBe gr.extRef
            res.comment mustBe gr.comment

          case bad =>
            fail(
              s"Expected an ${AnalysisCollection.getClass} but got an ${bad.getClass}"
            )
        }
      }

      "successfully update a result belonging to an analysis" in {
        val eid = EventId(6L)
        val ae  = dao.findById(defaultMid, eid).futureValue.successValue.value
        ae mustBe an[AnalysisCollection]
        ae.asInstanceOf[AnalysisCollection].result mustBe None

        val gr = dummyGenericResult(comment = Some("I'm a new result"))

        dao.upsertResult(defaultMid, eid, gr).futureValue.isSuccess mustBe true

        dao.findById(defaultMid, eid).futureValue.successValue.value match {
          case good: AnalysisCollection =>
            val res = good.result.value
            res.registeredBy mustBe gr.registeredBy
            res.registeredDate mustApproximate gr.registeredDate
            res.extRef mustBe gr.extRef
            res.comment mustBe gr.comment

          case bad =>
            fail(
              s"Expected an ${AnalysisCollection.getClass} but got an ${bad.getClass}"
            )
        }
      }

      "fail when trying to add a result to a non-existing analysis" in {
        val gr  = dummyGenericResult()
        val res = dao.upsertResult(defaultMid, EventId(100), gr).futureValue
        res.isFailure mustBe true
        res mustBe a[MusitDbError]
      }

      "return analyses for a museum and specified collection" in {
        val orig =
          dao.findAnalysisEvents(defaultMid, collections).futureValue.successValue
        // Add an analysis to KHM
        saveAnalysis(Some(oid2), Some(dummyGenericResult()), MuseumId(3))

        val res =
          dao.findAnalysisEvents(defaultMid, collections).futureValue.successValue

        res.size mustBe orig.size

        forAll(res) { event =>
          event mustBe a[AnalysisCollection]
          event.partOf mustBe None
        }
      }
    }

    "updating an analysis event" should {

      "successfully save the modified fields" in {
        val eid = EventId(1L)
        val ae = dao
          .findById(defaultMid, eid)
          .futureValue
          .successValue
          .value
          .asInstanceOf[Analysis]

        val upd = ae.copy(
          updatedBy = Some(dummyActorId),
          updatedDate = Some(dateTimeNow),
          note = Some("I was just updated")
        )

        val res = dao.update(defaultMid, eid, upd).futureValue.successValue.value

        res.note mustBe upd.note
        res.updatedBy mustBe Some(dummyActorId)
        res.updatedDate mustApproximate Some(dateTimeNow)
      }

      "fail if the analysis doesn't exist" in {
        val eid = EventId(200)
        val a   = dummyAnalysis(ObjectUUID.generateAsOpt()).copy(id = Some(eid))

        dao.update(defaultMid, eid, a).futureValue.isFailure mustBe true
      }
    }

  }
}
