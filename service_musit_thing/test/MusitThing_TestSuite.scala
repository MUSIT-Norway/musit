/**
  * Created by ellenjo on 4/15/16.
  */

import no.uio.musit.microservice.service_musit_thing.domain.MusitThing

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import no.uio.musit.microservices.common.PlayDatabaseTest
import no.uio.musit.microservice.service_musit_thing.dao.MusitThingDao
import org.scalatest.concurrent.ScalaFutures

import scala.util.{Failure, Success}

class MusitThing_TestSuite extends PlayDatabaseTest with ScalaFutures{

  import MusitThingDao._

  println("Før")
  //val dao = new MusitThingDao()
  println("Etter")

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
    testFutureMusitThing(insert,MusitThing(1, "C2", "spyd"))
    testFutureMusitThing(insert,MusitThing(2, "C3", "øks"))
    val svar=MusitThingDao.all()
    svar.onFailure{
      case ex => println(s"Feil i selectAll ${ex.getMessage}")
    }
    svar.map(things=> {
      println("Før loop")
      things.foreach(thing=>println(s"ID: ${thing.displayid}"))
      println("Etter loop")
    })
  }


  /*test("testInsertMusitThing") {
    insert(MusitThing(1, "C2", "spyd")).onFailure{case ex => println(s"Feil i insert1 ${ex.getMessage}")}
    insert(MusitThing(2, "C3", "øks")).onFailure{case ex => println(s"Feil i insert2 ${ex.getMessage}")}
    val svar=MusitThingDao.all()
    svar.onFailure{
      case ex => println(s"Feil i selectAll ${ex.getMessage}")
    }
    svar.map(things=> {
      println("Før loop")
      things.foreach(thing=>println(s"ID: ${thing.displayid}"))
      println("Etter loop")
      })
  }
*/
  testFuture("getDisplayName_kjempeTall",MusitThingDao.getDisplayName,6386363673636335366L,None)
  testFuture("test getDisplayName_Riktig", MusitThingDao.getDisplayName, 2, Some("øks"))
  testFuture("test getDisplayName_TalletNull",MusitThingDao.getDisplayName,0,None)

  testFuture("getDisplayID_kjempeTall",MusitThingDao.getDisplayID,6386363673636335366L,None)
  testFuture("test getDisplayID_Riktig", MusitThingDao.getDisplayID, 2, Some("C3"))
  testFuture("test getDisplayID_TalletNull",MusitThingDao.getDisplayID,0,None)

  testFuture("test getById_kjempeTall",MusitThingDao.getById,6386363673636335366L,None)
  testFuture("test getById__Riktig", MusitThingDao.getById, 1, Some(MusitThing(1,"C2","spyd")))
  testFuture("test getById__TalletNull",MusitThingDao.getById,0,None)

  /*
      test("test 1") {
        val verdi=5
        println("Ferdig1")

        assert(verdi==7)
      }
    test("test 3") {
      val verdi=5
      println("Ferdig3")

      assert(verdi==7)
    }*/
}
