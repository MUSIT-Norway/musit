package no.uio.musit.service

import akka.stream.Materializer
import com.google.inject.Inject
import play.api.Logger
import play.api.http.HeaderNames
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent.Future

class AccessLogFilter @Inject()(implicit val mat: Materializer) extends Filter {

  val logger = Logger("accesslog")

  type NextFilter = RequestHeader => Future[Result]

  def apply(nextFilter: NextFilter)(rh: RequestHeader): Future[Result] = {

    val startTime = System.currentTimeMillis

    logger.debug(s"Incoming request entering access log filter...")
    logger.debug(s"With request headers:\n${rh.headers.toSimpleMap.mkString("\n")}")

    nextFilter(rh).map { response =>
      val endTime  = System.currentTimeMillis
      val procTime = endTime - startTime

      logger.info(
        s"${rh.remoteAddress} - ${response.header.status} - " +
          s"${rh.method} ${rh.uri} - " +
          s"$procTime ms - " +
          s"${rh.host} - " +
          s"${rh.headers.get(HeaderNames.USER_AGENT).getOrElse("NA")}"
      )

      response.withHeaders("Processing-Time" -> procTime.toString)
    }
  }
}
