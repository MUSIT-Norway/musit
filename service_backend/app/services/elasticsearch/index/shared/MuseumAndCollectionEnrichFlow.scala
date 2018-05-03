package services.elasticsearch.index.shared

import akka.NotUsed
import akka.stream.scaladsl.Flow
import no.uio.musit.models.MuseumCollections.Collection
import no.uio.musit.models.{MuseumId, ObjectUUID}
import play.api.Logger
import repositories.elasticsearch.dao.ElasticsearchObjectsDao

import scala.concurrent.ExecutionContext

trait MuseumAndCollectionEnrichFlow[In, Out] {

  /**
   * Extract the ObjectUUID from the input message
   */
  def extractObjectUUID(input: In): Option[ObjectUUID]

  /**
   * Merge the input document with the set of
   */
  def mergeToOutput(
      input: In,
      midAndColl: Option[(MuseumId, Collection)]
  ): Out

  /**
   * Configurable async fetch operations.
   */
  val asyncFetch: Int = 1

  /**
   * The amount of input messages that should be grouped before fetching actors.
   */
  val groupedInputMsgSize: Int = 100

  private[this] val logger = Logger(classOf[MuseumAndCollectionEnrichFlow[In, Out]])

  def flow(
      implicit objectDao: ElasticsearchObjectsDao,
      ec: ExecutionContext
  ): Flow[In, Out, NotUsed] =
    Flow[In]
      .grouped(groupedInputMsgSize)
      .mapAsync(asyncFetch) { inputs =>
        val ouuids: Set[ObjectUUID] = inputs.flatMap(extractObjectUUID).toSet
        try {
          val set = ouuids
          objectDao.findObjectsMidAndCollection(set).map { items =>
            inputs.map(input => {
              val inputId = extractObjectUUID(input)
              val midAndCol =
                items.find(i => inputId.contains(i._1)).map(r => (r._2, r._3))
              mergeToOutput(input, midAndCol)
            })
          }
        } catch {
          case e: Exception => {
            logger.error(s" inni FLOW ")
            logger.error(s" flow UUIDs: ${ouuids}")
            throw (e)
          }
        }
      }
      .mapConcat(identity)
}
