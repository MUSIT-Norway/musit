/**
  * Created by ellenjo on 4/15/16.
  */

import no.uio.musit.microservice.service_musit_thing.dao.MusitThingDao
import no.uio.musit.microservice.service_musit_thing.domain.MusitThing
import no.uio.musit.microservices.common.PlayDatabaseTest
import no.uio.musit.microservices.common.linking.domain.Link
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MusitThing_TestSuite extends PlayDatabaseTest with ScalaFutures{

  import MusitThingDao._

  def testFuture[T](testnavn: String, f: Long => Future[T], verdi:Long, forventetSvar:T) = {
    test(testnavn) {
      val fut = f(verdi)
      whenReady(fut) { result =>
        assert(result == forventetSvar)
      }
    }
  }

  def testFutureMusitThing[T](f :MusitThing => Future[T],i_verdi:MusitThing)= {
    f(i_verdi).onFailure { case ex => println(s"Feil i insert1 ${ex.getMessage}")
    }
  }

  test("testInsertMusitThing") {
    testFutureMusitThing(insert,MusitThing(1, "C2", "spyd", Seq.empty))
    testFutureMusitThing(insert,MusitThing(2, "C3", "øks", Seq.empty))
    val svar=MusitThingDao.all()
    svar.onFailure{
      case ex => println(s"Feil i selectAll ${ex.getMessage}")
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
  testFuture("test getById__Riktig", MusitThingDao.getById, 1, Some(MusitThing(1,"C2","Kniv7", Seq(Link(-1,-1,"self", "/v1/1")))))
  testFuture("test getById__TalletNull",MusitThingDao.getById,0,None)

}
