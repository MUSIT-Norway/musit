package services.elasticsearch

import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

import scala.collection.mutable

/**
 * Group up elements from the stream until the limit is reached. Then it transform the
 * elements and reduce the origin elements from the stream.
 *
 * Typical use case it to transform the stream items where we need to group the them
 * for performance when calling IO.
 */
class GroupAndEnrichStage[In, Out, A, B](
    group: In => Set[A],
    transform: Set[A] => Set[B],
    reducer: (In, Set[B]) => Out,
    limit: Int
) extends GraphStage[FlowShape[In, Out]] {

  val in  = Inlet[In]("bulkQueryGraph.in")
  val out = Outlet[Out]("bulkQueryGraph.out")

  override def shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes) =
    new GraphStageLogic(shape) {
      val itemsToFetch   = Set.newBuilder[A]
      val elementToMerge = Vector.newBuilder[In]
      val buffer         = mutable.Queue[Out]()

      setHandler(out, new OutHandler {
        override def onPull() = {
          if (buffer.nonEmpty) {
            push(out, buffer.dequeue())
          } else {
            pull(in)
          }
        }
      })

      setHandler(
        in,
        new InHandler {
          override def onPush() = {
            if (buffer.isEmpty) {
              val next  = grab(in)
              val ies   = group(next)
              val items = itemsToFetch.result()

              if (items.nonEmpty && (ies.size + items.size) > limit) {
                fetchAndQueue()
                push(out, buffer.dequeue())
              } else {
                pull(in)
              }
              elementToMerge += next
              itemsToFetch ++= ies
            } else {
              push(out, buffer.dequeue())
            }
          }

          private def fetchAndQueue() = {
            val oes = transform(itemsToFetch.result())
            elementToMerge.result().foreach(e => buffer.enqueue(reducer(e, oes)))
            itemsToFetch.clear()
            elementToMerge.clear()
          }

          override def onUpstreamFinish() = {
            fetchAndQueue()
            buffer.dequeueAll(e => {
              emit(out, e)
              true
            })
            completeStage()
          }
        }
      )

      override def postStop() = {
        itemsToFetch.clear()
        elementToMerge.clear()
      }
    }

}
