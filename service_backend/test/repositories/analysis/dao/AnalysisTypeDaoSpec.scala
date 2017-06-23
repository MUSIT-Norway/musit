package repositories.analysis.dao

import java.util.UUID

import models.analysis.events.AnalysisTypeId
import models.analysis.events.EventCategories.Dating
import no.uio.musit.models.{ActorId, GroupId, MuseumId}
import no.uio.musit.models.MuseumCollections.Entomology
import no.uio.musit.security._
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import org.scalatest.Inspectors.forAll

class AnalysisTypeDaoSpec extends MusitSpecWithAppPerSuite with MusitResultValues {

  val dao: AnalysisTypeDao = fromInstanceCache[AnalysisTypeDao]

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
        museumId = MuseumId(99),
        description = None,
        collections = Seq()
      )
    )
  )

  "The AnalysisTypeDao" should {
    "return all the stored analysis types" in {
      val res = dao.all.futureValue

      val ats = res.successValue
      ats.size mustBe 46
      ats.map(_.category.entryName).distinct.size mustBe 13
    }

    "return all analysis types in the Dating category" in {
      val res = dao.allForCategory(Dating).futureValue

      val ats = res.successValue
      ats.size mustBe 3

      val expResAttrs1 = Map(
        "ageEstimate"       -> "String",
        "standardDeviation" -> "String"
      )

      val expResAttrs2 = Map("age" -> "String")

      forAll(ats) { a =>
        a.category mustBe Dating
        a.extraResultAttributes must contain oneOf (expResAttrs1, expResAttrs2)
      }
    }

    "return all analysis types for a specific collection" ignore {
      val entoUUID = Entomology.uuid // scalastyle:ignore

      val res = dao.allForCollection(entoUUID).futureValue

      val ats = res.successValue
      ats.size mustBe 28

      forAll(ats) { t =>
        (t.collections.contains(entoUUID) || t.collections.isEmpty) mustBe true
      }
    }

    "include specific result type and extra attributes for analysis type 'Counts & measurements'" in {
      val res = dao.allFor(None, None).futureValue

      val ats = res.successValue
      ats.size mustBe 46

      val measurementType = ats(39)
      measurementType.id mustBe AnalysisTypeId(40) // ;)
      measurementType.extraResultType mustBe Some("MeasurementResult")
      measurementType.extraResultAttributes.isEmpty mustBe false
      val extraResultAttributes = measurementType.extraResultAttributes.get
      extraResultAttributes.get("measurementType") mustBe Some("String")
      extraResultAttributes.get("method") mustBe Some("String")
      extraResultAttributes.get("precision") mustBe Some("String")
      extraResultAttributes.get("size") mustBe Some("Size")
    }
  }

}
