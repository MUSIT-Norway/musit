package repositories.musitsearchobject.dao

import java.util.UUID

import no.uio.musit.models.MuseumCollections.{Archeology, Collection, Ethnography, Lichen}
import no.uio.musit.models._
import no.uio.musit.security._
import no.uio.musit.test.MusitSpecWithServerPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import org.joda.time._

import scala.concurrent.ExecutionContext.Implicits.global

//Hint, to run only this test, type:
//test-only repositories.musitsearchobject.dao.SearchObjectDaoSpec

class SearchObjectDaoSpec
    extends MusitSpecWithServerPerSuite
    with MusitResultValues
    /*with MusitSearchObjectDao*/ {

  val dao: MusitSearchObjectDao = fromInstanceCache[MusitSearchObjectDao]

  val mid = MuseumId(99)

  def collectionToMuseumCollection(name: String, coll: Collection): MuseumCollection = {
    MuseumCollection(coll.uuid, Some(name), Seq(coll))

  }

  val allCollections = Seq(
    collectionToMuseumCollection("Arkeologi", Archeology),
    collectionToMuseumCollection("Etnografi", Ethnography),
    collectionToMuseumCollection("Lav", Lichen)
  )

  val dummyUid = ActorId.generate()

  implicit val dummyUser = AuthenticatedUser(
    session = UserSession(
      uuid = SessionUUID.generate(),
      oauthToken = Option(BearerToken(UUID.randomUUID().toString)),
      userId = Option(dummyUid),
      isLoggedIn = true
    ),
    userInfo = UserInfo(
      id = dummyUid,
      secondaryIds = Some(Seq("vader@starwars.com")),
      name = Some("Darth Vader"),
      email = None,
      picture = None
    ),
    groups = Seq(
      GroupInfo(
        id = GroupId.generate(),
        name = "FooBarGroup",
        module = StorageFacility,
        permission = Permissions.Admin,
        museumId = mid,
        description = None,
        collections = allCollections
      )
    )
  )
  override def beforeTests(): Unit = {
    println("<daotests, building search table>")
    val res = dao.recreateSearchTable().futureValue
    println("</daotests, building search table>")

  }

  val escapeChar = dao.escapeChar

  "The MusitSearchObjectDao" when {

    //This is copied from the SearchObjectDaoSpec
    "parsing intervals" should {

      import repositories.musitobject.dao.SearchFieldValues._

      val separator = dao.intervalSeparator

      "handle 5..7" in {
        separator mustBe ".." //Else the tests needs to be rewritten
        val res = dao.intervalFromString("5..7").successValue
        res.from mustBe Value(5)
        res.to mustBe Value(7)

      }
      "handle ..7" in {
        separator mustBe ".." //Else the tests needs to be rewritten
        val res = dao.intervalFromString("..7").successValue
        res.from mustBe Infinite()
        res.to mustBe Value(7)

      }
      "handle 5.." in {
        separator mustBe ".." //Else the tests needs to be rewritten
        val res = dao.intervalFromString("5..").successValue
        res.from mustBe Value(5)
        res.to mustBe Infinite()

      }

      "handle 5000000000..5000000001 (too long for Int)" in {
        separator mustBe ".." //Else the tests needs to be rewritten
        val res = dao.intervalFromString("5000000000..5000000001").successValue
        res.from mustBe Value(5000000000L)
        res.to mustBe Value(5000000001L)

      }

      "fail with x.." in {
        separator mustBe ".." //Else the tests needs to be rewritten
        val res = dao.intervalFromString("x..")
        res.isFailure mustBe true
      }
      "fail with ..x" in {
        separator mustBe ".." //Else the tests needs to be rewritten
        val res = dao.intervalFromString("..x")
        res.isFailure mustBe true
      }
      "fail with x..y" in {
        separator mustBe ".." //Else the tests needs to be rewritten
        val res = dao.intervalFromString("x..y")
        res.isFailure mustBe true
      }
      "fail with xy (precondition error (IllegalArgumentException)" in {
        val thrown = intercept[IllegalArgumentException] {
          dao.intervalFromString("xy")
        }

      }
    }

    "classifying search criteria" should {

      def checkWildcard(arg: String, expected: String) = {
        val res = dao.classifyValue(Some(arg))
        res.value.v mustBe expected
      }

      "replace '%' with the escape character" in {
        checkWildcard("C*_A", s"C%${escapeChar}_A")
      }

      "replace '*' with '%' and '%' with the escape character" in {
        checkWildcard("C*%A", s"C%$escapeChar%A")
      }

      "replace '*' with '%' and prefix '_' with the escape character" in {
        checkWildcard("*_", s"%${escapeChar}_")
      }

      "replace '*' with '%'" in {
        checkWildcard("C*A", "C%A")
      }

      "not prefix a single'%' with the escape character" in {
        checkWildcard("%", "%")
      }

      "not prefix a single '_' with the escape character" in {
        checkWildcard("_", "_")
      }

      "find musNr C1" in {
        val res = dao
          .executeSearch(
            99,
            Some(MuseumNo("C1")),
            None,
            None,
            None,
            allCollections,
            1,
            10
          )
          .value
          .futureValue
          .successValue
        //println(s"Total matches: ${res.totalMatches} data: ${res.matches}")
        res.totalMatches must be > 0

      }

      "find musNr C1 with museumNo_AsNumber search" in {
        val res = dao
          .executeSearch(
            99,
            Some(MuseumNo("C*")),
            Some("1"),
            None,
            None,
            allCollections,
            1,
            10
          )
          .value
          .futureValue
          .successValue
        res.totalMatches must be > 0

      }

      "find rows for museumNo_AsNumber 234-235 interval search" in {
        val res = dao
          .executeSearch(
            99,
            Some(MuseumNo("*")),
            Some("234..235"),
            None,
            None,
            allCollections,
            1,
            10
          )
          .value
          .futureValue
          .successValue
        //println(s"Total matches: ${res.totalMatches} data: ${res.matches}")
        res.totalMatches must be >= 2

      }
      "find more rows for museumNo_AsNumber 234-235 than search for 234 and for 235 (separately)" in {
        val res234_235 = dao
          .executeSearch(
            99,
            Some(MuseumNo("*")),
            Some("234..235"),
            None,
            None,
            allCollections,
            1,
            10
          )
          .value
          .futureValue
          .successValue
        res234_235.totalMatches must be >= 2

        val res234 = dao
          .executeSearch(
            99,
            Some(MuseumNo("*")),
            Some("234"),
            None,
            None,
            allCollections,
            1,
            10
          )
          .value
          .futureValue
          .successValue
        res234.totalMatches must be < res234_235.totalMatches

        val res235 = dao
          .executeSearch(
            99,
            Some(MuseumNo("*")),
            Some("235"),
            None,
            None,
            allCollections,
            1,
            10
          )
          .value
          .futureValue
          .successValue
        res235.totalMatches must be < res234_235.totalMatches

        val res4 = dao
          .executeSearch(
            99,
            Some(MuseumNo("*")),
            Some("234..234"),
            None,
            None,
            allCollections,
            1,
            10
          )
          .value
          .futureValue
          .successValue
        res4.totalMatches mustBe res234.totalMatches

      }

      "find musNr C81.. with museumNo_AsNumber interval search" in {
        val res = dao
          .executeSearch(
            99,
            Some(MuseumNo("C*")),
            Some("81.."),
            None,
            None,
            allCollections,
            1,
            10
          )
          .value
          .futureValue
          .successValue
        res.totalMatches must be >= 22 //At the moment we have 22 rows with C and 81..

      }

      "find musNr C1 and iterate over the result" in {
        val res = dao
          .executeSearch(
            99,
            Some(MuseumNo("C1")),
            None,
            None,
            None,
            allCollections,
            1,
            10
          )
          .value
          .futureValue
          .successValue

        val totalMatchCount = res.totalMatches
        res.totalMatches must be > 2

        val pageSize = totalMatchCount / 3

        val page1 = dao
          .executeSearch(
            99,
            Some(MuseumNo("C1")),
            None,
            None,
            None,
            allCollections,
            1,
            pageSize
          )
          .value
          .futureValue
          .successValue
        page1.matches.size mustBe pageSize
        page1.totalMatches mustBe totalMatchCount

        val page2 = dao
          .executeSearch(
            99,
            Some(MuseumNo("C1")),
            None,
            None,
            None,
            allCollections,
            2,
            pageSize
          )
          .value
          .futureValue
          .successValue
        page2.matches.size mustBe pageSize
        page2.totalMatches mustBe totalMatchCount

        val page3 = dao
          .executeSearch(
            99,
            Some(MuseumNo("C1")),
            None,
            None,
            None,
            allCollections,
            3,
            pageSize
          )
          .value
          .futureValue
          .successValue
        page3.matches.size must be <= pageSize
        page3.totalMatches mustBe totalMatchCount

        val page4 = dao
          .executeSearch(
            99,
            Some(MuseumNo("C1")),
            None,
            None,
            None,
            allCollections,
            5,
            pageSize
          )
          .value
          .futureValue
          .successValue
        page4.matches.size must be <= pageSize
        page4.totalMatches mustBe totalMatchCount

        val page5 = dao
          .executeSearch(
            99,
            Some(MuseumNo("C1")),
            None,
            None,
            None,
            allCollections,
            5,
            pageSize
          )
          .value
          .futureValue
          .successValue
        page5.matches.size mustBe 0
        page5.totalMatches mustBe totalMatchCount

      }

      "recreateSearchTable" in {
        val res = dao.recreateSearchTable().futureValue
      }
      "updateSearchTable" in {
        val res =
          dao.updateSearchTable(new org.joda.time.DateTime(1990, 1, 1, 0, 0)).futureValue
      }
    }
  }
}
