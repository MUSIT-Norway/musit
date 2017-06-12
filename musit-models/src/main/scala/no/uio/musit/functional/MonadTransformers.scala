package no.uio.musit.functional

import no.uio.musit.MusitResults.{
  MusitError,
  MusitInternalError,
  MusitResult,
  MusitSuccess
}

import scala.concurrent.Future

object MonadTransformers {

  /**
   * Monad transformer for MusitResult. Makes it easier to perform do map/flatMap
   * on MusitResults that are nested inside other Monadic types.
   *
   * @param value The value to perform monadic operations on
   * @param m     The Monad type this transformer works with
   * @tparam T The type of the wrapping Monad
   * @tparam A The type we want to access inside the transformed functions
   */
  case class MusitResultT[T[_], A](value: T[MusitResult[A]])(implicit m: Monad[T]) {

    def map[B](f: A => B): MusitResultT[T, B] =
      MusitResultT[T, B](m.map(value)(_.map(f)))

    def flatMap[B](f: A => MusitResultT[T, B]): MusitResultT[T, B] = {
      val res: T[MusitResult[B]] = m.flatMap(value) { a =>
        a.map(b => f(b).value).getOrElse {
          a match {
            case err: MusitError => m.pure(err)
            case o =>
              m.pure(
                MusitInternalError(
                  s"Unable to map into MusitResult in the MusitResultT transformer " +
                    s"because of $o"
                )
              )
          }
        }
      }
      MusitResultT[T, B](res)
    }

  }

  object MusitResultT {

    /**
     * Returns a MusitResultT[Future, A] where the Future is successfully
     * evaluated with the given MusitResult[A] argument.
     *
     * @param res the MusitResult to wrap
     * @param m an implicitly provided Monad[Future]
     * @tparam A the type we want to access inside the transformer functions
     * @return {{{MusitResultT[Future, A]}}}
     */
    def successful[A](
        res: MusitResult[A]
    )(implicit m: Monad[Future]): MusitResultT[Future, A] = {
      MusitResultT(Future.successful(res))
    }

    def successful[A](
        res: A
    )(implicit m: Monad[Future]): MusitResultT[Future, A] = {
      MusitResultT(Future.successful[MusitResult[A]](MusitSuccess(res)))
    }

  }

  /**
   * Monad transformer for Option. Makes coding with nested monadic types
   * (map/flatMap) easier.
   *
   * @param value The value to perform monadic operations on
   * @param m     The Monad type this transformer works with
   * @tparam T The type of the wrapping Monad
   * @tparam A The type we want to access inside the transformed functions
   */
  case class OptionT[T[_], A](value: T[Option[A]])(implicit m: Monad[T]) {

    def map[B](f: A => B): OptionT[T, B] =
      OptionT[T, B](m.map(value)(_.map(f)))

    def flatMap[B](f: A => OptionT[T, B]): OptionT[T, B] = {
      val res: T[Option[B]] = m.flatMap(value) { a =>
        a.map(b => f(b).value).getOrElse(m.pure(None))
      }
      OptionT[T, B](res)
    }

  }

}
