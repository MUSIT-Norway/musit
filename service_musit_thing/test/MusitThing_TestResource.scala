/**
  * Created by ellenjo on 4/20/16.
  */

import play._
import play.api.libs.ws.ning.NingWSClient
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util._

object MusitThing_TestResource extends App{


  val url = "http://localhost:8080/v1"
  val ws = NingWSClient()


  def insertThing(id: Long, displayid: String ,displayname: String) = {

    val body =s"""{"id":  $id, "displayid": "$displayid", "displayname": "$displayname"}"""
    println("Skal sette inn: "+body)
    ws.url(url).withHeaders("Content-Type" -> "application/json").post(
      body)
  }

  var result  = for {
    //_ <- ws.url(url).get()
    svar <- insertThing(1, "C1", "Øks")
    svar <- insertThing(1, "C2", "Spyd")

//    svar4 <- deletePerson(ws,3)
//    svar5 <- test(ws)

    _=println("Etter test")
/*
    _ = println("post"+ svar.body)
    _ = println("post"+ svar2.body)
    _ = println("post"+ svar3.body)
    _ = println("ferdig")
    _= println("delete "+svar4.body)
    */
  } ws.close()
  Thread.sleep(2000)
}
