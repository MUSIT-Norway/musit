package services.elasticsearch.shared

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import no.uio.musit.models.ActorId
import no.uio.musit.test.MusitSpecWithApp
import services.actor.ActorService

import scala.concurrent.ExecutionContext

class ActorEnrichFlowSpec extends MusitSpecWithApp {

  val service = fromInstanceCache[ActorService]
  val ec      = fromInstanceCache[ExecutionContext]

  implicit val as  = fromInstanceCache[ActorSystem]
  implicit val mat = ActorMaterializer()

  "ActorEnrichFlow" must {

    "transform to output message when actor exist" in {
      val flow = new ActorEnrichFlow[InputMsg, OutputMsg] {
        override def extractActorsId(input: InputMsg) = Set(input.id)

        override def mergeWithActors(input: InputMsg, actors: Set[(ActorId, String)]) =
          OutputMsg(input.id, actors.toMap.get(input.id))

      }.flow(service, ec)

      val actorId =
        ActorId.fromUUID(UUID.fromString("41ede78c-a6f6-4744-adad-02c25fb1c97c"))
      val outMsg = Source
        .single(InputMsg(actorId))
        .via(flow)
        .runWith(Sink.head[OutputMsg])
        .futureValue

      outMsg mustBe OutputMsg(actorId, Some("And, Arne1"))
    }

    "transform to output message when actor does not exist" in {
      val flow = new ActorEnrichFlow[InputMsg, OutputMsg] {
        override def extractActorsId(input: InputMsg) = Set(input.id)

        override def mergeWithActors(input: InputMsg, actors: Set[(ActorId, String)]) =
          OutputMsg(input.id, actors.toMap.get(input.id))

      }.flow(service, ec)

      val actorId = ActorId.generate()
      val outMsg = Source
        .single(InputMsg(actorId))
        .via(flow)
        .runWith(Sink.head[OutputMsg])
        .futureValue

      outMsg mustBe OutputMsg(actorId, None)
    }

  }

  case class InputMsg(id: ActorId)
  case class OutputMsg(id: ActorId, name: Option[String])
}
