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
class ObjectSearchDaoSpec extends MusitSpecWithAppPerSuite {
  val dao: ObjectSearchDao = fromInstanceCache[ObjectSearchDao]

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  val escapeChar = dao.escapeChar

  def insertTestData(museumId: Int) = {
    def insert(museumNo: String, subNo: String, term: String) = {
      dao.testInsert(museumId, MusitThing(
        museumNo = museumNo,
        subNo = if (subNo.isEmpty) None else Some(subNo),
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

    insert("C555", "34B", "Øks")
    insert("C555", "34A", "Øks")
    insert("C555", "34C", "Øks")
    insert("C555A", "B", "Øks")
    insert("C555B", "A", "Øks")
    insert("C555C", "C", "Øks")

    insert("C888_B", "B", "Øks")
    insert("C888_A", "A", "Øks")
    insert("C888xC", "C", "Øks")

    insert("C81%A", "A", "Bøtte")
    insert("C81%XA", "B", "Bøtte")

    insert("C81-A", "A", "Bøtte")
    insert("C81-XA", "B", "Bøtte")

    insert(s"C81${escapeChar}A", "A", "Bøtte")
    insert(s"C81${escapeChar}XA", "B", "Bøtte")

  }

  "ObjectSearch" must {
    "dummy test to insert test data" in {

    }
    "find an object which exists, via museumNo" in {
      insertTestData(1)

      val res = dao.search(1, "C1", "", "", 1, 100).futureValue
      val seq = res.get
      assert(seq.length >= 3)

      val res2 = dao.search(1, "C2", "", "", 1, 100).futureValue
      val seq2 = res2.get
      assert(seq2.length > 0)

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
    seq.length mustBe 6

    seq(0).subNo mustBe Some("34A")
    seq(1).subNo mustBe Some("34B")
    seq(2).subNo mustBe Some("34C")
    seq(3).museumNo mustBe "C555A"
    seq(4).museumNo mustBe "C555B"
    seq(5).museumNo mustBe "C555C"

  }

  "simple SQL-injection attempt should return 0 rows" in {
    val res = dao.search(1, "C.' or 1=1 --", "", "", 1, 10).futureValue
    val seq = res.get
    seq.length mustBe 0
  }

  "museumNo, subNo with * and term search should work" in {

    val res = dao.search(1, "c555*", "3*", "øks", 1, 10).futureValue
    val seq = res.get

    seq.length mustBe 3
    seq(0).subNo mustBe Some("34A")
    seq(1).subNo mustBe Some("34B")
    seq(2).subNo mustBe Some("34C")
  }

  "museumNo with * and _ should work" in {

    val res = dao.search(1, "c888_*", "", "øks", 1, 10).futureValue
    val seq = res.get

    seq.length mustBe 2

    seq(0).museumNo mustBe "C888_A"
    seq(1).museumNo mustBe "C888_B"
  }

  "that % is treated like an ordinary character in =-context" in {
    val res = dao.search(1, "C81%A", "", "", 1, 10).futureValue
    val seq = res.get

    seq.length mustBe 1 //We should find C81%A and *not* C81%XA
    seq(0).museumNo mustBe "C81%A"
  }

  "that % is treated like an ordinary character in like-context" in {
    val res = dao.search(1, "C*%A", "", "", 1, 10).futureValue
    val seq = res.get

    seq.length mustBe 1 //We should find C81%A and *not* C81%XA
    seq(0).museumNo mustBe "C81%A"
  }

  "that - is treated like an ordinary character in =-context" in {
    val res = dao.search(1, "C81-A", "", "", 1, 10).futureValue
    val seq = res.get

    seq.length mustBe 1 //We should find C81%A and *not* C81%XA
    seq(0).museumNo mustBe "C81-A"
  }

  "that - is treated like an ordinary character in like-context" in {
    val res = dao.search(1, "C*-A", "", "", 1, 10).futureValue
    val seq = res.get

    seq.length mustBe 1 //We should find C81%A and *not* C81%XA
    seq(0).museumNo mustBe "C81-A"
  }

  "that the escape character is treated like an ordinary character in =-context" in {
    val res = dao.search(1, s"C81${escapeChar}A", "", "", 1, 10).futureValue
    val seq = res.get

    seq.length mustBe 1 //We should find C81¤A and *not* C81¤XA
    seq(0).museumNo mustBe s"C81${escapeChar}A"
  }

  "that the esacpe character is treated like an ordinary character in like-context" in {
    val res = dao.search(1, s"C*${escapeChar}A", "", "", 1, 10).futureValue
    val seq = res.get

    seq.length mustBe 1 //We should find C81%A and *not* C81%XA
    seq(0).museumNo mustBe s"C81${escapeChar}A"
  }

  "that an escape clause is generated because it is needed" in {
    dao.testSearchSql(1, s"C*_A", "", "", 1, 10).get must include("escape")
    dao.testSearchSql(1, s"C*%A", "", "", 1, 10).get must include("escape")

    dao.testSearchSql(1, s"", "*_", "", 1, 10).get must include("escape")
    dao.testSearchSql(1, s"", "*%", "", 1, 10).get must include("escape")

    dao.testSearchSql(1, s"", "", "*_", 1, 10).get must include("escape")
    dao.testSearchSql(1, s"", "", "*%", 1, 10).get must include("escape")
  }

  "that an escape clause is not generated if not needed" in {
    dao.testSearchSql(1, s"C*A", "", "", 1, 10).get mustNot include("escape")

    dao.testSearchSql(1, s"C*A", "%", "_", 1, 10).get mustNot include("escape")
  }
}
