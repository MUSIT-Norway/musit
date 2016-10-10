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

      insert("C1", "1a", "Øks")
      insert("C1", "2a", "Skummel øks")
      insert("C1", "3", "Fin øks")
      insert("C1", "4", "Fin øks")
      insert("C1", "5", "Fin øks")
      insert("C1", "6", "Fin øks")
      insert("C1", "7", "Fin øks")
      insert("C1", "8", "Fin øks")
      insert("C1", "9", "Fin øks")
      insert("C1", "10a", "Fin øks")
      insert("C1", "11", "Fin øks")
      insert("C1", "12", "Fin øks")
      insert("C1", "13", "Fin øks")
      insert("C1", "14", "Fin øks")
      insert("C1", "15", "Fin øks")
      insert("C1", "16", "Fin øks")
      insert("C1", "17", "Fin øks")
      insert("C1", "18", "Fin øks")
      insert("C1", "19", "Fin øks")
      insert("C1", "20b", "Fin øks")
      insert("C1", "22", "Fin øks") //Note: Deliberately inserting 22 before 21, to check if sorting works!
      insert("C1", "21", "Fin øks")
      insert("C2", "", "Sverd")

      insert("C777", "35", "Øks")
      insert("C.777", "34B", "Øks")
      insert("C.777", "34", "Øks")
      insert("C.777", "34A", "Øks")

      insert("C555A", "34B", "Øks")
      insert("C555B", "34A", "Øks")
      insert("C555C", "34C", "Øks")
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

    "offsets and sorting must work ok" in {

      val res = dao.search(1, "C1", "", "", 1, 10).futureValue
      val seq = res.get
      seq.length mustBe 10

      seq(0).subNo mustBe Some("1a")
      seq(1).subNo mustBe Some("2a")

      val res2 = dao.search(1, "C1", "", "", 2, 10).futureValue
      val seq2 = res2.get
      seq2.length mustBe 10

      val res3 = dao.search(1, "C1", "", "", 3, 10).futureValue
      val seq3 = res3.get

      seq3.length mustBe 2

      seq3(0).subNo mustBe Some("21")
      seq3(1).subNo mustBe Some("22")
    }
  }

  "museumNo with digits only" in {

    val res = dao.search(1, "777", "", "", 1, 10).futureValue
    val seq = res.get
    seq.length mustBe 4

    seq(0).subNo mustBe Some("34")
    seq(1).subNo mustBe Some("34A")
    seq(2).subNo mustBe Some("34B")
    seq(3).subNo mustBe Some("35")
  }

  "museumNo with * should work" in {

    val res = dao.search(1, "C555*", "", "", 1, 10).futureValue
    val seq = res.get
    seq.length mustBe 3

    seq(0).subNo mustBe Some("34A")
    seq(1).subNo mustBe Some("34B")
    seq(2).subNo mustBe Some("34C")
  }

  "museumNo with % should fail" in {
    val res = dao.search(1, "C.%", "", "", 1, 10).futureValue
    res.isFailure mustBe true
  }


  "museumNo, subNo with * and term search should work" in {

    val res = dao.search(1, "c*", "3*", "øks", 1, 10).futureValue
    val seq = res.get

    seq.length mustBe 3
    seq(0).subNo mustBe Some("34A")
    seq(1).subNo mustBe Some("34B")
    seq(2).subNo mustBe Some("34C")
  }


}
