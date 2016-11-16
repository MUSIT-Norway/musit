package no.uio.musit.service

import javax.inject.Inject

import akka.stream.Materializer
import play.api.http.{HeaderNames, Status}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class NoCacheFilter @Inject() (
    implicit val mat: Materializer,
    ec: ExecutionContext
) extends Filter {

  def apply(next: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    next(rh).map(response =>
      response.header.status match {
        case Status.NOT_MODIFIED => response
        case _ =>
          response.withHeaders(
            HeaderNames.CACHE_CONTROL -> "no-cache,no-store,max-age=0"
          )
      })
  }
}