import scala.concurrent.Future

sealed trait MusitResult[E, T]

case class MusitError[E, T](err: E) extends MusitResult[E, T]

case class MusitSuccess[E, T](res: T) extends MusitResult[E, T]


case class FutureMusitResult[E, T](fmr: Future[MusitResult[E, T]]) {

  val underlying = fmr

  def map   ...fmr
  def flatMap   ...

  def toFutureOption = map(_.toOption)
}

