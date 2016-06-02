package no.uio.musit.microservices.common.utils

/**
 * Created by ellenjo on 6/2/16.
 */
object Misc {

  implicit class Pipe[T](x: T) {
    def |>[U](f: T => U): U = f(x)
  }

}
