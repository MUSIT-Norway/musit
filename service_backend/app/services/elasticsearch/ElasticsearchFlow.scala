package services.elasticsearch

import akka.NotUsed
import akka.event.Logging
import akka.stream.Attributes
import akka.stream.scaladsl.Flow
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.bulk.BulkResponse
import no.uio.musit.healthcheck.StopWatch
import play.api.Logger
import com.sksamuel.elastic4s.http.ElasticDsl._

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext

class ElasticsearchFlow(
    client: HttpClient,
    maxBulkSize: Int
)(implicit ec: ExecutionContext) {

  val logger = Logger(classOf[ElasticsearchFlow])

  def flow(name: String): Flow[BulkCompatibleDefinition, BulkResponse, NotUsed] =
    Flow[BulkCompatibleDefinition]
      .grouped(maxBulkSize)
      .via(startStopWatch)
      .mapAsync(1)(ba => client.execute(bulk(ba._1)).map((_, ba._2)))
      .via(logStopWatch(name))

  private def startStopWatch[A] = Flow[Seq[A]].map((_, StopWatch()))

  private def logStopWatch(
      name: String
  ): Flow[(BulkResponse, StopWatch), BulkResponse, NotUsed] =
    Flow[(BulkResponse, StopWatch)]
      .log(s"stream.elasticsearch.$name", r => toLog(r._2, r._1))
      .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))
      .map(_._1)

  private def toLog(sw: StopWatch, res: BulkResponse) = {
    res.failures.foreach(item => logger.warn(item.error.toString))
    s"Elasticsearch responded in ${sw.elapsed()} ms," +
      s" successes: ${res.successes.size}," +
      s" failures: ${res.failures.size}, took: ${res.took}"
  }

}
