package repositories.musitsearchobject.dao

import java.util.UUID

import no.uio.musit.models._
import no.uio.musit.security._
import no.uio.musit.test.MusitSpecWithServerPerSuite
import no.uio.musit.test.matchers.MusitResultValues

import scala.concurrent.ExecutionContext.Implicits.global

//Hint, to run only this test, type:
//test-only repositories.musitsearchobject.dao.SearchObjectDaoSpec

class SearchObjectDaoSpec
    extends MusitSpecWithServerPerSuite
    with MusitResultValues
    /*with MusitSearchObjectDao*/ {

  val dao: MusitSearchObjectDao = fromInstanceCache[MusitSearchObjectDao]

  val mid = MuseumId(99)

  val allCollections = Seq(
    MuseumCollection(
      uuid = MuseumCollections.Archeology.uuid,
      name = Some("Arkeologi"),
      oldSchemaNames = Seq(MuseumCollections.Archeology)
    )
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
          .executeSearch(99, Some(MuseumNo("C1")), None, None, allCollections, 1, 10)
          .value
          .futureValue
          .successValue
        //println(s"Total matches: ${res.totalMatches} data: ${res.matches}")
        res.totalMatches must be > 0

      }

      "find musNr C1 and iterate over the result" in {
        val res = dao
          .executeSearch(99, Some(MuseumNo("C1")), None, None, allCollections, 1, 10)
          .value
          .futureValue
          .successValue

        val totalMatchCount = res.totalMatches
        res.totalMatches must be > 2

        //println(s"Total matches: ${res.totalMatches} data: ${res.matches}")

        val pageSize = totalMatchCount / 3

        val page1 = dao
          .executeSearch(
            99,
            Some(MuseumNo("C1")),
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
    }
  }
}
