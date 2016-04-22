/**
  * Created by ellenjo on 4/20/16.
  */

import no.uio.musit.microservices.common.PlayDatabaseTest
import org.scalatest.concurrent.ScalaFutures
import play._
import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util._


class MusitThing_TestResource extends PlayDatabaseTest with ScalaFutures {

  def applyEvolutions(ws: NingWSClient, portnr: Int, vnr: Int) = {
    println("Applying evolutions...")
    ws.url(s"http://localhost:$portnr/@evolutions/apply/default?redirect=http://localhost:$portnr/v$vnr").get()
  }

  def maybeApplyEvolutions(ws: NingWSClient, portnr: Int, vnr: Int) = {
    val url = s"http://localhost:$portnr/v$vnr"
    ws.url(url).get().map(resp => resp.body.contains("needs evolution!")).map {
      applyEvo => if (applyEvo) {

        applyEvolutions(ws, portnr, vnr)
        true
      }
      else false
    }
  }

  val url = "http://localhost:7070/v1"
  val ws = NingWSClient()

  def insertThing(id: Long, displayid: String, displayname: String) = {
    val body =s"""{"id":  $id, "displayid": "$displayid", "displayname": "$displayname"}"""
    println("Skal sette inn5: " + body)
    ws.url(url).withHeaders("Content-Type" -> "application/json").post(
      body)

  }


  def getThing(i: Int) = {
    val svar = ws.url(s"$url/$i").withHeaders("Content-Type" -> "application/json").get().map(_.body)
    svar
  }


  val result = maybeApplyEvolutions(ws, 7070, 1).map { mustInit =>
    if (mustInit) {
      for {
        svar <- insertThing(1, "C1", "Øks5")
      //svar2 <- insertThing(2, "C2", "Spyd5")
      } yield svar
      for {
      // svar <- insertThing(1, "C1", "Øks5")
        svar2 <- insertThing(2, "C2", "Spyd5")
      } yield svar2
    }
    for {
      svar3 <- getThing(1)
      _ = println(s"json: $svar3")

    } yield svar3
  }

  result.onComplete {
    case Success(s) => println("Success!")
    case Failure(ex) => println(s"Tryna! :(   ${ex.getMessage}")
  }


  val svar = result.futureValue

  ws.close()

}
