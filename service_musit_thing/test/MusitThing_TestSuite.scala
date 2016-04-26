/**
  * Created by ellenjo on 4/15/16.
  */

import no.uio.musit.microservice.service_musit_thing.dao.MusitThingDao
import no.uio.musit.microservice.service_musit_thing.domain.MusitThing
import no.uio.musit.microservices.common.PlayDatabaseTest
import no.uio.musit.microservices.common.linking.LinkService
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MusitThing_TestSuite extends PlayDatabaseTest with ScalaFutures{

  import MusitThingDao._

  def testFuture[T](testName: String, f: Long => Future[T], value:Long, expectedAnswer:T) = {
    test(testName) {
      val fut = f(value)
      whenReady(fut) { result =>
        assert(result == expectedAnswer)
      }
    }
  }

  def testFutureMusitThing[T](f :MusitThing => Future[T], value:MusitThing)= {
    f(value).onFailure {
      case ex => fail("Insert failed")
    }
  }

  test("testInsertMusitThing") {
    testFutureMusitThing(insert,MusitThing(1, "C2", "spyd", Seq.empty))
    testFutureMusitThing(insert,MusitThing(2, "C3", "øks", Seq.empty))
    val svar=MusitThingDao.all()
    svar.onFailure{
      case ex => fail("Insert failed")
    }
    whenReady(svar) { things =>
      assert (things.length == 4)
    }
  }

  testFuture("getDisplayName_kjempeTall",MusitThingDao.getDisplayName,6386363673636335366L,None)
  testFuture("test getDisplayName_Riktig", MusitThingDao.getDisplayName, 2, Some("Kniv7"))
  testFuture("test getDisplayName_TalletNull",MusitThingDao.getDisplayName,0,None)

  testFuture("getDisplayID_kjempeTall",MusitThingDao.getDisplayID,6386363673636335366L,None)
  testFuture("test getDisplayID_Riktig", MusitThingDao.getDisplayID, 2, Some("C2"))
  testFuture("test getDisplayID_TalletNull",MusitThingDao.getDisplayID,0,None)

  testFuture("test getById_kjempeTall",MusitThingDao.getById,6386363673636335366L,None)
  testFuture("test getById__Riktig", MusitThingDao.getById, 1, Some(MusitThing(1,"C1","Øks5", Seq(LinkService.self("/v1/1")))))
  testFuture("test getById__TalletNull",MusitThingDao.getById,0,None)

}
