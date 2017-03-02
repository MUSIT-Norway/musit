package repositories.dao

import models.events.AnalysisResults.{AnalysisResult, DatingResult, GeneralResult}
import models.events.{Analysis, AnalysisCollection, AnalysisTypeId}
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.models.{ActorId, EventId, ObjectUUID}
import no.uio.musit.time.dateTimeNow
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.Inspectors.forAll

class AnalysisDaoSpec extends MusitSpecWithAppPerSuite {

  val dao: AnalysisDao = fromInstanceCache[AnalysisDao]

  val dummyActorId = ActorId.generate()
  val dummyAnalysisTypeId = AnalysisTypeId.generate()

  val oid1 = ObjectUUID.generate()
  val oid2 = ObjectUUID.generate()
  val oid3 = ObjectUUID.generate()

  def dummyAnalysisWithRes(
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

  def saveAnalysisWithResult(
    oid: Option[ObjectUUID],
    res: Option[AnalysisResult]
  ): MusitResult[EventId] = {
    val a = dummyAnalysisWithRes(oid, res)
    dao.insert(a).futureValue
  }

  "AnalysisEventDao" when {

    "inserting analysis events" should {
      "return the EventId allocated to a single analysis" in {
        val res = Some(GeneralResult(
          registeredBy = Some(dummyActorId),
          registeredDate = Some(dateTimeNow),
          extRef = Some(Seq("foo", "bar", "fizz")),
          comment = Some("This is a result comment")
        ))

        saveAnalysisWithResult(Some(oid1), res) mustBe MusitSuccess(EventId(1))
      }

      "return the EventId allocated for an analysis collection" in {
        val now = Some(dateTimeNow)

        val e1 = dummyAnalysisWithRes(Some(oid1))
        val e2 = dummyAnalysisWithRes(Some(oid2))
        val e3 = dummyAnalysisWithRes(Some(oid3))

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
      "allow for fetching all child events for an analysis collection" in {
        val children = dao.findByCol(EventId(2)).futureValue
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
        val gr = Some(GeneralResult(
          registeredBy = Some(dummyActorId),
          registeredDate = Some(dateTimeNow),
          extRef = Some(Seq("shizzle", "in", "the", "drizzle")),
          comment = None
        ))

        val mra = saveAnalysisWithResult(Some(oid2), gr)
        mra.isSuccess mustBe true

        val res = dao.findById(mra.get).futureValue
        res.isSuccess mustBe true
        res.get must not be empty
        res.get.get.registeredBy mustBe Some(dummyActorId)
        res.get.get.analysisTypeId mustBe dummyAnalysisTypeId
        res.get.get.result mustBe gr
      }

      "return the an analysis event with a dating result" in {
        val dr = Some(DatingResult(
          registeredBy = Some(dummyActorId),
          registeredDate = Some(dateTimeNow),
          extRef = None,
          comment = None,
          age = Some("Ancient stuff")
        ))

        val mra = saveAnalysisWithResult(Some(oid2), dr)
        mra.isSuccess mustBe true

        val res = dao.findById(mra.get).futureValue
        res.isSuccess mustBe true
        res.get must not be empty
        res.get.get.registeredBy mustBe Some(dummyActorId)
        res.get.get.analysisTypeId mustBe dummyAnalysisTypeId
        res.get.get.result mustBe dr
      }

      "return all analysis events for a given object" in {
        val res = dao.findByObjectUUID(oid2).futureValue

        res.isSuccess mustBe true
        res.get.size mustBe 3
      }
    }

  }

}
