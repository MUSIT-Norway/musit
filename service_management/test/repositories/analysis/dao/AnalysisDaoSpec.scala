package repositories.analysis.dao

import models.analysis.events.AnalysisResults.{
  AnalysisResult,
  DatingResult,
  GenericResult
}
import models.analysis.events.{Analysis, AnalysisCollection, AnalysisTypeId}
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.models.{ActorId, EventId, ObjectUUID}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.time.dateTimeNow
import no.uio.musit.test.matchers.{DateTimeMatchers, MusitResultValues}
import org.scalatest.Inspectors.forAll
import org.scalatest.OptionValues

class AnalysisDaoSpec
    extends MusitSpecWithAppPerSuite
    with DateTimeMatchers
    with MusitResultValues
    with OptionValues {

  val dao: AnalysisDao = fromInstanceCache[AnalysisDao]

  val dummyActorId        = ActorId.generate()
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
      doneBy = Some(dummyActorId),
      doneDate = now,
      registeredBy = Some(dummyActorId),
      registeredDate = now,
      responsible = Some(dummyActorId),
      administrator = Some(dummyActorId),
      updatedBy = Some(dummyActorId),
      updatedDate = now,
      completedBy = Some(dummyActorId),
      completedDate = now,
      partOf = None,
      objectId = oid,
      note = Some("This is the first event"),
      result = res
    )
  }

  def dummyAnalysisCollection(
      res: Option[AnalysisResult],
      analyses: Analysis*
  ): AnalysisCollection = {
    val now = Some(dateTimeNow)
    AnalysisCollection(
      id = None,
      analysisTypeId = dummyAnalysisTypeId,
      doneBy = Some(dummyActorId),
      doneDate = now,
      registeredBy = Some(dummyActorId),
      registeredDate = now,
      note = Some("An analysis collection"),
      result = res,
      events = analyses.toSeq
    )
  }

  def saveAnalysis(
      oid: Option[ObjectUUID],
      res: Option[AnalysisResult]
  ): MusitResult[EventId] = {
    val a = dummyAnalysis(oid, res)
    dao.insert(a).futureValue
  }

  "AnalysisDao" when {

    "inserting analysis events" should {
      "return the EventId allocated to a single analysis" in {
        val res = Some(
          GenericResult(
            registeredBy = Some(dummyActorId),
            registeredDate = Some(dateTimeNow),
            extRef = Some(Seq("foo", "bar", "fizz")),
            comment = Some("This is a result comment")
          )
        )

        saveAnalysis(Some(oid1), res) mustBe MusitSuccess(EventId(1))
      }

      "return the EventId allocated for an analysis collection" in {
        val now = Some(dateTimeNow)

        val gr = Some(
          GenericResult(
            registeredBy = Some(dummyActorId),
            registeredDate = Some(dateTimeNow),
            extRef = Some(Seq("foo", "bar", "fizz")),
            comment = Some("This is a result comment")
          )
        )

        val e1 = dummyAnalysis(Some(oid1))
        val e2 = dummyAnalysis(Some(oid2))
        val e3 = dummyAnalysis(Some(oid3))

        val ac = dummyAnalysisCollection(gr, e1, e2, e3)

        val res = dao.insertCol(ac).futureValue

        res.successValue mustBe EventId(2)
      }
    }

    "fetching analysis events" should {

      "return an analysis collection with children" in {
        val res = dao.findById(EventId(2)).futureValue

        res.successValue.value mustBe an[AnalysisCollection]
        val ac = res.successValue.value.asInstanceOf[AnalysisCollection]
        ac.events must not be empty
        ac.analysisTypeId mustBe dummyAnalysisTypeId
        ac.partOf mustBe empty
      }

      "return each child in a collection separately by fetching them with their IDs" in {
        val res1 = dao.findById(EventId(3)).futureValue
        val res2 = dao.findById(EventId(4)).futureValue
        val res3 = dao.findById(EventId(5)).futureValue

        res1.successValue.value.partOf mustBe Some(EventId(2))
        res2.successValue.value.partOf mustBe Some(EventId(2))
        res3.successValue.value.partOf mustBe Some(EventId(2))
      }

      "list all child events for an analysis collection" in {
        val children = dao.listChildren(EventId(2)).futureValue
        children.successValue.size mustBe 3

        forAll(children.successValue) { child =>
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

        val res = dao.findById(mra.successValue).futureValue
        res.successValue.value mustBe an[Analysis]
        val a = res.successValue.value.asInstanceOf[Analysis]
        a.registeredBy mustBe Some(dummyActorId)
        a.analysisTypeId mustBe dummyAnalysisTypeId
        a.result.value.comment mustBe gr.comment
        a.result.value.extRef mustBe gr.extRef
        a.result.value.registeredBy mustBe gr.registeredBy
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

        val res = dao.findById(mra.successValue).futureValue
        res.successValue.value mustBe an[Analysis]
        val analysis = res.successValue.value.asInstanceOf[Analysis]
        analysis.registeredBy mustBe Some(dummyActorId)
        analysis.analysisTypeId mustBe dummyAnalysisTypeId
        analysis.result.value mustBe a[DatingResult]
        val datingResult = analysis.result.value.asInstanceOf[DatingResult]
        datingResult.comment mustBe dr.comment
        datingResult.extRef mustBe dr.extRef
        datingResult.registeredBy mustBe dr.registeredBy
        datingResult.age mustBe dr.age
      }

      "return all analysis events for a given object" in {
        val res = dao.findByObjectUUID(oid2).futureValue

        res.successValue.size mustBe 3
      }
    }

  }

}
