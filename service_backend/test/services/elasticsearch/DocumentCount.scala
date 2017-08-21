package services.elasticsearch

import com.sksamuel.elastic4s.IndexesAndTypes
import com.sksamuel.elastic4s.http.{HttpExecutable, ResponseHandler}
import org.elasticsearch.client.RestClient

import scala.concurrent.Future

object DocumentCount {

  case class CountDefinition(indexesAndTypes: IndexesAndTypes)

  case class CountResponse(count: Int)

  implicit object CountHttpExecutable
      extends HttpExecutable[CountDefinition, CountResponse] {
    override def execute(
        client: RestClient,
        request: CountDefinition
    ): Future[CountResponse] = {

      import scala.concurrent.ExecutionContext.Implicits._

      val index =
        if (request.indexesAndTypes.indexes.nonEmpty)
          request.indexesAndTypes.indexes.mkString(",")
        else "_all"

      val types = request.indexesAndTypes.types.mkString("/", ",", "")

      val endpoint = s"/$index$types/_count"

      client
        .async("GET", endpoint, Map.empty, ResponseHandler.failure404)
        .map(res => res)
    }
  }

  def count(index: String) =
    CountDefinition(IndexesAndTypes(index))

  def count(index: String, typ: String) =
    CountDefinition(IndexesAndTypes(index, typ))

}
