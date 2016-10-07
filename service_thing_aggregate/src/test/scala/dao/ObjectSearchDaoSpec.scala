package dao

import com.google.inject.Inject
import models.MusitThing
import no.uio.musit.test.{MusitSpecWithAppPerSuite}
import org.scalatest.time.{Millis, Seconds, Span}

import no.uio.musit.service.MusitResults.MusitSuccess
import models.{MuseumIdentifier, ObjectId}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.Logger

/**
  * Created by jarle on 07.10.16.
  */
class ObjectSearchDaoSpec extends MusitSpecWithAppPerSuite{


  /*
  @Inject() (
                                      val dao: ObjectSearchDao
                                    )
   */

  val dao: ObjectSearchDao = fromInstanceCache[ObjectSearchDao]


  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )
    def insertTestData(museumId: Int) = {
      def insert(museumNo: String, subNo: String, term: String) = {
        dao.testInsert(museumId, MusitThing(
          museumNo = museumNo,
          subNo = if(subNo.isEmpty) None else Some(subNo),
          term = term
        )).futureValue
      }

      insert("C1", "a", "Øks")
      insert("C1", "1", "Skummel øks")
      insert("C1", "2", "Fin øks")
      insert("C2", "", "Sverd")
      insert("C777", "34", "Øks")
    }


  "ObjectSearch" must {
    "dummy test to insert test data" in {

    }
    "find an object which exists, via museumNo" in {
      insertTestData(1)

      val res = dao.search(1, "C1", "", "", 1, 100).futureValue
      val seq = res.get
      assert(seq.length>=3)

      val res2 = dao.search(1, "C2", "", "", 1, 100).futureValue
      val seq2 = res2.get
      assert(seq2.length>0)

    }
  }

}
