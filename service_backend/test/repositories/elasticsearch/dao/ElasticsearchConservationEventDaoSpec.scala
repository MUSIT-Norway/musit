package repositories.elasticsearch.dao

import java.util.UUID

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.models.{CollectionUUID, EventId, MuseumId, ObjectUUID}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import no.uio.musit.time
import org.joda.time.DateTime
import repositories.conservation.dao.MaterialDeterminationDao
import utils.testdata.BaseDummyData
import repositories.conservation.dao.TreatmentDao
import services.elasticsearch.index.Indexer
import utils.testdata.ConservationprocessGenerators

//Hint, to run only this test, type:
//test-only repositories.elasticsearch.dao.ElasticsearchConservationEventDaoSpec

class ElasticsearchConservationEventDaoSpec
    extends MusitSpecWithAppPerSuite
    with ConservationprocessGenerators
    with MusitResultValues
    with BaseDummyData {

  val esEventDao                       = fromInstanceCache[ElasticSearchConservationEventDao]
  private val materialDeterminationDao = fromInstanceCache[MaterialDeterminationDao]

  def getEventStream(date: Option[DateTime]) = {

    esEventDao.conservationEventStream(
      date,
      Indexer.defaultFetchsize,
      esEventDao.defaultEventProvider
    )
  }

  def saveMaterialDetermination(
      oids: Option[Seq[ObjectUUID]],
      mid: MuseumId = defaultMid
  ): MusitResult[EventId] = {
    val mde = dummyMaterialDetermination(oids)
    materialDeterminationDao.insert(mid, mde).value.futureValue
  }

  "ElasticsearchConservationEventDao" should {

    val treatmentDao = fromInstanceCache[TreatmentDao]
    implicit val mat = fromInstanceCache[Materializer]

    "publish all events" in {

      val source   = getEventStream(None)
      val resStart = source.runWith(Sink.seq).futureValue

      val startSize = resStart.size.toLong

      saveMaterialDetermination(None)

      val sourceAfter = getEventStream(None)
      val resAfter    = sourceAfter.runWith(Sink.seq).futureValue

      val nextSize = startSize + 1
      resAfter must have size nextSize

    }
    "conservationevents should include object uuid" in {
      val oid1   = ObjectUUID.unsafeFromString("2350578d-0bb0-4601-92d4-817478ad0952")
      val matDet = dummyMaterialDetermination(Some(Seq(oid2, oid1)))
      materialDeterminationDao.insert(mid, matDet).value.futureValue

      val source   = getEventStream(None)
      val resSeqMr = source.runWith(Sink.seq).futureValue
      val res      = MusitResult.sequence(resSeqMr).successValue
      val objectUuids = res
        .map(_.event.affectedThings)
        .collect {
          case Some(seq) => seq
        }
        .flatten

      objectUuids must contain only (
        ObjectUUID.fromUUID(UUID.fromString("2350578d-0bb0-4601-92d4-817478ad0952")),
        oid2
      )
    }

    "publish after a given timestamp" in {
      val id = EventId(2)

      val evt =
        materialDeterminationDao
          .findSpecificConservationEventById(defaultMid, id)
          .value
          .futureValue
          .successValue
          .value

      Thread.sleep(2) //Sleep two milliseconds, to make sure all previous updates are at least 2 milliseconds in the past
      val now = time.dateTimeNow.minusMillis(1) //Just to make sure the getEventStream will include the below and *only* the below
      materialDeterminationDao.update(defaultMid, id, evt).value.futureValue.successValue

      val source = getEventStream(Some(now))
      val res    = source.runWith(Sink.seq).futureValue

      res must have size 1
    }

    "publish after a given timestamp in the future, shouldn't find anything" in {
      val id = EventId(2)

      val evt =
        materialDeterminationDao
          .findSpecificConservationEventById(defaultMid, id)
          .value
          .futureValue
          .successValue
          .value

      //println(evt)

      materialDeterminationDao.update(defaultMid, id, evt).value.futureValue.successValue
      val now = time.dateTimeNow.plusSeconds(5) //Just to make sure the getEventStream will *NOT* get anything

      val source = getEventStream(Some(now))
      val res    = source.runWith(Sink.seq).futureValue

      res must have size 0
    }

  }
}
