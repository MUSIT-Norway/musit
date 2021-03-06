package repositories.elasticsearch.dao

import java.util.UUID

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import models.analysis.events.{Analysis, AnalysisCollection}
import models.elasticsearch.AnalysisSearchType
import no.uio.musit.models.{EventId, GroupId, ObjectUUID}
import no.uio.musit.security._
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import no.uio.musit.time
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

      val source = esEventDao.analysisEventsStream()
      val res    = source.runWith(Sink.seq).futureValue

      res must have size 3
    }

    "analysis should include object uuid" in {
      val gr = Some(dummyGenericResult())
      val e1 = dummyAnalysis(Some(oid1))
      val e2 = dummyAnalysis(Some(oid2))
      val ac = dummyAnalysisCollection(gr, e1, e2)
      analysisDao.insertCol(defaultMid, ac).futureValue.successValue

      val source = esEventDao.analysisEventsStream()
      val res    = source.runWith(Sink.seq).futureValue

      val analysisObjectUuids = res.flatMap {
        case a: AnalysisSearchType => a.objectUuid
        case _                     => None
      }

      analysisObjectUuids must contain only (
        ObjectUUID.fromUUID(UUID.fromString("2e5037d5-4952-4571-9de2-709eb22b01f0")),
        ObjectUUID.fromUUID(UUID.fromString("4d2e516d-db5f-478e-b409-eac7ff2486e8"))
      )
    }

    "publish after a give timestamp" in {
      val id = EventId(1)
      val evt =
        analysisDao.findAnalysisById(defaultMid, id).futureValue.successValue.value

      val now = time.dateTimeNow
      val updEvt = evt match {
        case a: AnalysisCollection => a.copy(updatedDate = Some(now.plusMinutes(5)))
        case a: Analysis           => a.copy(updatedDate = Some(now.plusMinutes(5)))
      }

      analysisDao.update(defaultMid, id, updEvt).futureValue.successValue

      val source = esEventDao.analysisEventsStream(Some(now))
      val res    = source.runWith(Sink.seq).futureValue

      res must have size 1
    }
  }

}
