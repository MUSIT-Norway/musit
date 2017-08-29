package services.elasticsearch.index.shared

import akka.NotUsed
import akka.stream.scaladsl.Flow
import no.uio.musit.models.ActorId
import services.actor.ActorService

import scala.concurrent.{ExecutionContext, Future}

trait ActorEnrichFlow[In, Out] {

  /**
   * Extract the actor ids from the input message
   */
  def extractActorsId(input: In): Set[ActorId]

  /**
   * Merge the actors with the input message and return the result
   */
  def mergeWithActors(input: In, actors: Set[(ActorId, String)]): Out

  /**
   * Configurable async fetch operations.
   */
  val asyncFetch: Int = 1

  /**
   * The amount of input messages that should be grouped before fetching actors.
   */
  val groupedInputMsgSize: Int = 100

  private[this] def findActors(
      actors: Set[ActorId]
  )(
      implicit actorService: ActorService,
      ec: ExecutionContext
  ): Future[Set[(ActorId, String)]] =
    actorService.findDetails(actors).map { ps =>
      ps.foldLeft(Map.empty[ActorId, String]) {
          case (state, per) =>
            val tmp = Map.newBuilder[ActorId, String]
            per.applicationId.foreach(id => tmp += id -> per.fn)
            per.dataportenId.foreach(id => tmp += id  -> per.fn)

            state ++ tmp.result()
        }
        .toSet
    }

  def flow(
      implicit actorService: ActorService,
      ec: ExecutionContext
  ): Flow[In, Out, NotUsed] =
    Flow[In]
      .map(input => (input, extractActorsId(input)))
      .grouped(groupedInputMsgSize)
      .mapAsync(asyncFetch)(inputActors => {
        val actorIds = inputActors.flatMap(_._2).toSet
        val inputs   = inputActors.map(_._1)
        findActors(actorIds).map(actors => inputs.map(in => mergeWithActors(in, actors)))
      })
      .mapConcat(identity)

}
