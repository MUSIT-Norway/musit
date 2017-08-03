package repositories.elasticsearch.dao

import java.util.UUID

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import no.uio.musit.models.GroupId
import no.uio.musit.security._
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import repositories.analysis.dao.AnalysisDao
import utils.testdata.AnalysisGenerators

class ElasticsearchEventDaoSpec
    extends MusitSpecWithAppPerSuite
    with AnalysisGenerators
    with MusitResultValues {
  "ElasticsearchEventDao" should {

    implicit val dummyUser = AuthenticatedUser(
      session = UserSession(
        uuid = SessionUUID.generate(),
        oauthToken = Option(BearerToken(UUID.randomUUID().toString)),
        userId = Option(dummyActorId),
        isLoggedIn = true
      ),
      userInfo = UserInfo(
        id = dummyActorId,
        secondaryIds = Some(Seq("vader@starwars.com")),
        name = Some("Darth Vader"),
        email = None,
        picture = None
      ),
      groups = Seq(
        GroupInfo(
          id = GroupId.generate(),
          name = "FooBarGroup",
          module = CollectionManagement,
          permission = Permissions.Admin,
          museumId = defaultMid,
          description = None,
          collections = Seq()
        )
      )
    )

    val esEventDao   = fromInstanceCache[ElasticsearchEventDao]
    val analysisDao  = fromInstanceCache[AnalysisDao]
    implicit val mat = fromInstanceCache[Materializer]

    "publish all events" in {
      val gr = Some(dummyGenericResult())
      val e1 = dummyAnalysis(Some(oid1))
      val e2 = dummyAnalysis(Some(oid2))
      val ac = dummyAnalysisCollection(gr, e1, e2)
      analysisDao.insertCol(defaultMid, ac).futureValue.successValue

      val pub = esEventDao.analysisEvents()

      val res = Source.fromPublisher(pub).runWith(Sink.seq).futureValue

      res must have size 3
    }
  }
}
