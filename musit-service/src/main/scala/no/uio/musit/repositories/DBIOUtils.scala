package no.uio.musit.repositories

import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

object DBIOUtils {

  def flatMapInsideOption[A, B](
      value: DBIO[Option[A]],
      f: A => DBIO[B]
  )(implicit ec: ExecutionContext): DBIO[Option[B]] = {

    value.flatMap {
      case Some(v) => {
        f(v).map(Some(_))
      }
      case None => {
        DBIO.successful[Option[B]](None)
      }
    }
  }

}
