package services.elasticsearch.index.shared

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import no.uio.musit.models.MuseumCollections.Collection
import no.uio.musit.models.{MuseumId, ObjectUUID}
import no.uio.musit.test.MusitSpecWithApp
import repositories.elasticsearch.dao.ElasticsearchThingsDao

import scala.concurrent.ExecutionContext

class MuseumAndCollectionEnrichFlowSpec extends MusitSpecWithApp {

  implicit val ec  = fromInstanceCache[ExecutionContext]
  implicit val mat = fromInstanceCache[Materializer]
  implicit val dao = fromInstanceCache[ElasticsearchThingsDao]

  "MuseumAndCollectionEnrichFlow" should {
    case class InputMsg(id: ObjectUUID)
    case class OutputMsg(o: ObjectUUID, mid: Option[MuseumId], col: Option[Collection])

    val enrich = new MuseumAndCollectionEnrichFlow[InputMsg, OutputMsg] {
      override def extractObjectUUID(input: InputMsg) = Some(input.id)

      override def mergeToOutput(input: InputMsg, mac: Option[(MuseumId, Collection)]) =
        OutputMsg(input.id, mac.map(_._1), mac.map(_._2))
    }

    "group up and enrich items" in {
      val objIdOne = ObjectUUID.fromString("89f36f77-2c27-4d33-81b4-4d4f9688950d").value
      val objIdTwo = ObjectUUID.fromString("42b6a92e-de59-4fde-9c46-5c8794be0b34").value
      val source   = Source(List(InputMsg(objIdOne), InputMsg(objIdTwo)))

      val res = source.via(enrich.flow).runWith(Sink.seq).futureValue

      res must contain only (
        OutputMsg(objIdOne, Some(MuseumId(99)), Some(Collection.fromInt(1))),
        OutputMsg(objIdTwo, Some(MuseumId(99)), Some(Collection.fromInt(4)))
      )

    }
  }

}
