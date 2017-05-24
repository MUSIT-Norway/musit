package migration

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import repositories.storage.dao.MigrationDao.AllEventsRow

// TODO: This file can be removed when Migration has been performed.

private[migration] final class EventConcat
    extends GraphStage[FlowShape[AllEventsRow, Seq[AllEventsRow]]] {

  val in  = Inlet[AllEventsRow]("AllEventsConcat.in")
  val out = Outlet[Seq[AllEventsRow]]("AllEventsConcat.out")

  override def shape = FlowShape.of(in, out)

  override def createLogic(attributes: Attributes) = new GraphStageLogic(shape) {

    // format: off
    private var currentState: Option[AllEventsRow] = None
    private val buffer = Vector.newBuilder[AllEventsRow]
    // format: on

    setHandlers(
      in = in,
      out = out,
      handler = new InHandler with OutHandler {
        override def onPush(): Unit = {
          val nextElement = grab(in)
          val nextId      = nextElement._1.id

          if (currentState.isEmpty || currentState.exists(_._1.id == nextId)) {
            buffer += nextElement
            pull(in)
          } else {
            val result = buffer.result()
            buffer.clear()
            buffer += nextElement
            push(out, result)
          }
          currentState = Some(nextElement)
        }

        override def onPull(): Unit = pull(in)

        override def onUpstreamFinish(): Unit = {
          val result = buffer.result()
          if (result.nonEmpty) emit(out, result)
          completeStage()
        }
      }
    )

    override def postStop(): Unit = buffer.clear()
  }
}
