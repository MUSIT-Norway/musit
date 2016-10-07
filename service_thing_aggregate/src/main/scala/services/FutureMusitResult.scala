package services

import no.uio.musit.service.MusitResults.{MusitError, MusitResult, MusitSuccess}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by jarle on 06.10.16.
 */
object FutureMusitResults {

  def flattenFMFM[A](fmfm: FutureMusitResult[FutureMusitResult[A]])(implicit ec: ExecutionContext): FutureMusitResult[A] = {
    val res = fmfm.underlying.flatMap {
      case MusitSuccess(succ) => succ.underlying
      case err: MusitError =>
        val r = Future.successful(err.asInstanceOf[MusitResult[A]])
        r
    }
    FutureMusitResult(res)
  }

  def invertMF[A](mf: MusitResult[Future[A]])(implicit ec: ExecutionContext): Future[MusitResult[A]] = {
    mf match {
      case MusitSuccess(succ) => succ.map(a=> MusitSuccess(a))
      case err: MusitError => Future.successful(err)
    }
  }


  case class FutureMusitResult[+A](val underlying: Future[MusitResult[A]]) {

    def map[B](f: A => B)(implicit ec: ExecutionContext): FutureMusitResult[B] = {
      FutureMusitResult(underlying.map { _.map(f) })
    }

    def flatMap[B](f: A => FutureMusitResult[B])(implicit ec: ExecutionContext): FutureMusitResult[B] = {
      flattenFMFM {
        map(f)
      }
    }

    /*
    def flatten[B](implicit ev: A <:< FutureMusitResult[B]): FutureMusitResult[B] = {
      flattenFMFM(this)
    }
*/
  }
}
