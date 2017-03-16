package repositories.analysis.dao

import models.analysis.events.AnalysisResults.{AnalysisResult, DatingResult, GenericResult}
import models.analysis.events.{Analysis, AnalysisCollection, AnalysisTypeId}
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.models.{ActorId, EventId, ObjectUUID}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.DateTimeMatchers
import no.uio.musit.time.dateTimeNow
import org.scalatest.Inspectors.forAll

class AnalysisDaoSpec extends MusitSpecWithAppPerSuite with DateTimeMatchers {

  val dao: AnalysisDao = fromInstanceCache[AnalysisDao]

  val dummyActorId = ActorId.generate()
  val dummyAnalysisTypeId = AnalysisTypeId.generate()

  val oid1 = ObjectUUID.generate()
  val oid2 = ObjectUUID.generate()
  val oid3 = ObjectUUID.generate()

  def dummyAnalysis(
    oid: Option[ObjectUUID],
    res: Option[AnalysisResult] = None
  ): Analysis = {
    val now = Some(dateTimeNow)
    Analysis(
      id = None,
      analysisTypeId = dummyAnalysisTypeId,
      eventDate = now,
      registeredBy = Some(dummyActorId),
      registeredDate = now,
      partOf = None,
      objectId = oid,
      note = Some("This is the first event"),
      result = res
    )
  }

  def saveAnalysis(
    oid: Option[ObjectUUID],
    res: Option[AnalysisResult]
  ): MusitResult[EventId] = {
    val a = dummyAnalysis(oid, res)
    dao.insert(a).futureValue
  }

  "AnalysisEventDao" when {

    "inserting analysis events" should {
      "return the EventId allocated to a single analysis" in {
        val res = Some(GenericResult(
          registeredBy = Some(dummyActorId),
          registeredDate = Some(dateTimeNow),
          extRef = Some(Seq("foo", "bar", "fizz")),
          comment = Some("This is a result comment")
        ))

        saveAnalysis(Some(oid1), res) mustBe MusitSuccess(EventId(1))
      }

      "return the EventId allocated for an analysis collection" in {
        val now = Some(dateTimeNow)

        val e1 = dummyAnalysis(Some(oid1))
        val e2 = dummyAnalysis(Some(oid2))
        val e3 = dummyAnalysis(Some(oid3))

        val ac = AnalysisCollection(
          id = None,
          analysisTypeId = dummyAnalysisTypeId,
          eventDate = now,
          registeredBy = Some(dummyActorId),
          registeredDate = now,
          events = Seq(e1, e2, e3)
        )

        val res = dao.insertCol(ac).futureValue

        res.isSuccess mustBe true
        res.get mustBe EventId(2)
      }
    }

    "fetching analysis events" should {

      "return an analysis collection with children" in {
        val res = dao.findById(EventId(2)).futureValue

        res.isSuccess mustBe true
        res.get must not be empty
        res.get.get mustBe an[AnalysisCollection]
        val ac = res.get.get.asInstanceOf[AnalysisCollection]
        ac.events must not be empty
        ac.analysisTypeId mustBe dummyAnalysisTypeId
        ac.partOf mustBe empty
      }

      "return each child in a collection separately by fetching them with their IDs" in {
        val res1 = dao.findById(EventId(3)).futureValue
        val res2 = dao.findById(EventId(4)).futureValue
        val res3 = dao.findById(EventId(5)).futureValue

        res1.isSuccess mustBe true
        res2.isSuccess mustBe true
        res3.isSuccess mustBe true

        res1.get must not be empty
        res2.get must not be empty
        res3.get must not be empty

        res1.get.get.partOf mustBe Some(EventId(2))
        res2.get.get.partOf mustBe Some(EventId(2))
        res3.get.get.partOf mustBe Some(EventId(2))
      }

      "list all child events for an analysis collection" in {
        val children = dao.listChildren(EventId(2)).futureValue
        children.isSuccess mustBe true
        children.get.size mustBe 3

        forAll(children.get) { child =>
          child.result mustBe None
          child.analysisTypeId mustBe dummyAnalysisTypeId
          child.partOf mustBe Some(EventId(2))
          child.registeredBy mustBe Some(dummyActorId)
        }
      }

      "return the analysis event that matches the given id" in {
        val gr = GenericResult(
          registeredBy = Some(dummyActorId),
          registeredDate = Some(dateTimeNow),
          extRef = Some(Seq("shizzle", "in", "the", "drizzle")),
          comment = None
        )

        val mra = saveAnalysis(Some(oid2), Some(gr))
        mra.isSuccess mustBe true

        val res = dao.findById(mra.get).futureValue
        res.isSuccess mustBe true
        res.get must not be empty
        res.get.get mustBe an[Analysis]
        val a = res.get.get.asInstanceOf[Analysis]
        a.registeredBy mustBe Some(dummyActorId)
        a.analysisTypeId mustBe dummyAnalysisTypeId
        a.result must not be empty
        a.result.get.comment mustBe gr.comment
        a.result.get.extRef mustBe gr.extRef
        a.result.get.registeredBy mustBe gr.registeredBy
      }

      "return the an analysis event with a dating result" in {
        val dr = DatingResult(
          registeredBy = Some(dummyActorId),
          registeredDate = Some(dateTimeNow),
          extRef = None,
          comment = None,
          age = Some("Ancient stuff")
        )

        val mra = saveAnalysis(Some(oid2), Some(dr))
        mra.isSuccess mustBe true

        val res = dao.findById(mra.get).futureValue
        res.isSuccess mustBe true
        res.get must not be empty
        res.get.get mustBe an[Analysis]
        val analysis = res.get.get.asInstanceOf[Analysis]
        analysis.registeredBy mustBe Some(dummyActorId)
        analysis.analysisTypeId mustBe dummyAnalysisTypeId
        analysis.result must not be empty
        analysis.result.get mustBe a[DatingResult]
        val datingResult = analysis.result.get.asInstanceOf[DatingResult]
        datingResult.comment mustBe dr.comment
        datingResult.extRef mustBe dr.extRef
        datingResult.registeredBy mustBe dr.registeredBy
        datingResult.age mustBe dr.age
      }

      "return all analysis events for a given object" in {
        val res = dao.findByObjectUUID(oid2).futureValue

        res.isSuccess mustBe true
        res.get.size mustBe 3
      }
    }

  }

}
