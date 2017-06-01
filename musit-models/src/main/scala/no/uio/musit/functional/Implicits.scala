package no.uio.musit.functional

import scala.annotation.implicitNotFound
import scala.concurrent.{ExecutionContext, Future}

object Implicits {

  /**
   * Implicit converter to wrap a {{{no.uio.musit.functional.Monad}}} around a
   * {{{scala.concurrent.Future}}}. This allows for composition of Monads using
   * Monad transformers.
   *
   * @param ec The ExecutionContext for mapping on the Future type
   * @return a Monad of type Future
   */
  @implicitNotFound(
    "A Future[Monad] needs an implicit ExecutionContext in " +
      "scope. If you are using the Play! Framework, consider using the " +
      "frameworks default context:\n" +
      "import play.api.libs.concurrent.Execution.Implicits.defaultContext\n\n" +
      "Otherwise, use the ExecutionContext that best suits your needs."
  )
  implicit def futureMonad(implicit ec: ExecutionContext) = new Monad[Future] {
    override def map[A, B](value: Future[A])(f: (A) => B) = value.map(f)

    override def flatMap[A, B](value: Future[A])(f: (A) => Future[B]) =
      value.flatMap(f)

    override def pure[A](x: A): Future[A] = Future(x)
  }
}
