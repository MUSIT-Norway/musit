package no.uio.musit.functional

import no.uio.musit.MusitResults.{
  MusitError,
  MusitResult,
  MusitSuccess,
  MusitValidationError
}

import scala.concurrent.{ExecutionContext, Future}

/* Some common functional signatures:


   Assume M is a typical structure having the given operation, the "elevated" world.
   (sometimes Monad is required, sometimes Applicative or Functor or something else)



  map[A, B]       M[A], f: A=>B       : M[B]

  filter[T]       M[T], f: T=>Bool    : M[T]

  fold[T]         M[T]                : T

  flatten[T]      M[M[T]]             : M[T]

  flatMap[A, B]   M[A], f: A=>M[B]    : M[B]

  sequence[T]     Seq[M[T]]           : M[Seq[T]]

  traverse[A,B]   Seq[A], f: A=> M[B] : M[Seq[B]]


  Some FutureMusitResult (FMR) and MusitResult (MR) signatures:
  (Better names are welcomed!)

  mapAndFlattenMusitResult[A,B]   FMR[A], f: A=MR[B]              : FMR[B]
  pairWise[A,B]                   Seq[A], f: A => FMR[Option[B]]  : FMR[Seq[(A, Option[B])]]
  collectAllOrFail[A, B]          Seq[A], f: A => FMR[Option[B]], Seq[A] => MusitError   : FMR[Seq[B]]

  getOrError  FMR[Option[T]],   => MusitError    : FMR[T]
  flatMapInsideOption[A,B]  FMR[Option[A]], f: A=>FMR[B]     : FRM[Option[B]]

 */

case class FutureMusitResult[A](value: Future[MusitResult[A]]) {
  def map[B](f: A => B)(implicit ec: ExecutionContext): FutureMusitResult[B] = {
    val res = value.map(_.map(f))
    FutureMusitResult(res)
  }

  def flatMap[B](f: A => FutureMusitResult[B])(implicit ec: ExecutionContext) = {
    val res = value.flatMap { musitResult =>
      musitResult match {
        case MusitSuccess(t) => f(t).value
        case err: MusitError => Future.successful(err)
      }
    }
    FutureMusitResult(res)
  }

  def mapAndFlattenMusitResult[B](
      f: A => MusitResult[B]
  )(implicit ec: ExecutionContext): FutureMusitResult[B] = {
    val temp = map(f).value
    val res  = temp.map(mrMr => mrMr.flatten)
    FutureMusitResult(res)
  }

}

object Extensions {

  implicit class FutMrOption[T](val value: FutureMusitResult[Option[T]]) extends AnyVal {

    def getOrError(
        error: => MusitError
    )(implicit ec: ExecutionContext): FutureMusitResult[T] = {
      value.mapAndFlattenMusitResult { opt =>
        opt.fold[MusitResult[T]](error)(t => MusitSuccess(t))
      }
    }

    def flatMapInsideOption[B](
        f: T => FutureMusitResult[B]
    )(implicit ec: ExecutionContext): FutureMusitResult[Option[B]] = {

      value.flatMap[Option[B]] {
        case Some(v) => {
          f(v).map(Some(_))
        }
        case None => {
          FutureMusitResult.successful[Option[B]](None)
        }
      }
    }
  }

  implicit class FutureExtensions[T](val value: Future[T]) extends AnyVal {

    def toMusitFuture()(implicit ec: ExecutionContext): FutureMusitResult[T] = {
      val temp = FutureMusitResult(value.map(MusitSuccess[T](_)))
      temp
    }
  }

}
object FutureMusitResult {
  def successful[A](
      a: A
  ): FutureMusitResult[A] = {
    FutureMusitResult(Future.successful(MusitSuccess(a)))
  }

  def failed[A](
      err: MusitError
  ): FutureMusitResult[A] = {
    FutureMusitResult(Future.successful[MusitResult[A]](err))
  }

  def from[A](mr: MusitResult[A]) = FutureMusitResult(Future.successful(mr))
  def from[A](a: A)               = successful(a)
  def from[A](futA: Future[A])(implicit ec: ExecutionContext) =
    FutureMusitResult(futA.map(a => MusitSuccess(a))) //Is this correct
  // or should we also do some recover and if fails return something else than MusitSuccess?

  def requireFromClient(test: Boolean, errorMsg: String): FutureMusitResult[Unit] = {
    if (test) {
      successful(())
    } else {
      failed(MusitValidationError(errorMsg))
    }
  }

  def flatten[A](
      futMrFutMr: FutureMusitResult[FutureMusitResult[A]]
  )(implicit ec: ExecutionContext) = {
    futMrFutMr.flatMap(identity)

  }

  def sequence[A](seqFutMr: Seq[FutureMusitResult[A]])(
      implicit ec: ExecutionContext
  ): FutureMusitResult[Seq[A]] = {
    FutureMusitResult(
      Future.sequence(seqFutMr.map(_.value)).map { results =>
        MusitResult.sequence(results)

      }
    )
  }

  def traverse[A, B](
      seqA: Seq[A],
      f: A => FutureMusitResult[B]
  )(implicit ec: ExecutionContext): FutureMusitResult[Seq[B]] = {
    val res = seqA.map(f)
    sequence(res)
  }

  // Some less standard helpers

  /** Gitt en liste med et eller annet, f.eks. ID-er og en funksjon som tar en slik Id og gir tilbake en
   * FutureMusitResult[Option[T]] (typisk et database-kall ala findById, getById etc), returnerer en liste med par av "id" og evt svar.
   * Merk at vi feiler om et (eller flere) av f-kallene gir en MusitError. Vi kjører uansett alle kallene, så feilene blir akkumulert.
   */
  def pairwise[A, B](
      seq: Seq[A],
      f: A => FutureMusitResult[Option[B]]
  )(implicit ec: ExecutionContext): FutureMusitResult[Seq[(A, Option[B])]] = {

    def makePair(a: A): FutureMusitResult[(A, Option[B])] = f(a).map(optB => (a, optB))

    traverse(seq, makePair)
  }

  /** Gitt en liste med et eller annet, f.eks. ID-er og en funksjon som tar en slik Id og gir tilbake en
   * FutureMusitResult[Option[T]] (typisk et database-kall ala findById, getById etc), returnerer en liste med de som ble funnet.
   * Merk at vi feiler om et (eller flere) av f-kallene gir en MusitError. Vi kjører uansett alle kallene, så feilene blir akkumulert.
   * Og vi feiler også dersom vi ikke finner alle objektene!
   */
  def collectAllOrFail[A, B](
      seqA: Seq[A],
      f: A => FutureMusitResult[Option[B]],
      errorFactory: Seq[A] => MusitError
  )(implicit ec: ExecutionContext): FutureMusitResult[Seq[B]] = {
    val pairs = pairwise(seqA, f)

    pairs.mapAndFlattenMusitResult { seqOfPairs =>
      val asWithoutBs = seqOfPairs.collect { case (a, None) => a }
      if (asWithoutBs.isEmpty) {
        MusitSuccess(seqOfPairs.collect { case (_, Some(b)) => b })
      } else errorFactory(asWithoutBs)
    }
  }
}
