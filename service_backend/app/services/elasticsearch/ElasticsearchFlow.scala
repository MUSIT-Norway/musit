package services.elasticsearch

import akka.NotUsed
import akka.event.Logging
import akka.stream.Attributes
import akka.stream.scaladsl.{Flow, Source}
import no.uio.musit.MusitResults.{MusitError, MusitHttpError, MusitResult, MusitSuccess}
import no.uio.musit.healthcheck.StopWatch
import play.api.Logger
import play.api.libs.json.Json
import services.elasticsearch.client.ElasticsearchIndicesApi
import services.elasticsearch.client.models.BulkActions.BulkAction
import services.elasticsearch.client.models.BulkResponse
import services.elasticsearch.client.models.ItemResponses.IndexItemResponse

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext

class ElasticsearchFlow(
    client: ElasticsearchIndicesApi,
    maxBulkSize: Int
)(implicit ec: ExecutionContext) {

  val logger = Logger(classOf[ElasticsearchFlow])

  def flow(): Flow[BulkAction, MusitResult[BulkResponse], NotUsed] =
    Flow[BulkAction]
      .grouped(maxBulkSize)
      .via(startStopWatch)
      .mapAsync(1)(ba => client.bulkAction(Source(ba._1)).map((_, ba._2)))
      .via(logStopWatch)

  private val startStopWatch = Flow[Seq[BulkAction]].map((_, StopWatch()))
  private val logStopWatch =
    Flow[(MusitResult[BulkResponse], StopWatch)]
      .log("stream.elasticsearch", r => toLog(r._2, r._1))
      .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))
      .map(_._1)

  private def toLog(sw: StopWatch, res: MusitResult[BulkResponse]) = {
    res match {
      case MusitSuccess(response) =>
        val results = response.items.foldLeft(Map[String, Int]()) { (aggCount, item) =>
          item match {
            case indexItem: IndexItemResponse =>
              val result = indexItem.result.getOrElse("error")
              indexItem.error.foreach { err =>
                logger.warn(
                  s"Document error, id=${indexItem.id} type=${indexItem.typ}" +
                    s" index=${indexItem.index}, error=${Json.stringify(err)}"
                )
              }
              val newCount = aggCount.getOrElse(result, 0) + 1
              aggCount + (result -> newCount)
            case _ => aggCount
          }
        }
        s"Elasticsearch responded in ${sw.elapsed()} ms, result: $results"
      case MusitHttpError(code, msg) =>
        s"Elasticsearch request failed with httpCode: $code, msg: $msg "
      case err: MusitError =>
        s"Unknown fault from Elasticsearch after ${sw.elapsed()} ms, msg: ${err.message}"
    }
  }

}
