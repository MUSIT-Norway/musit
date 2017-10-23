package services.elasticsearch

import com.google.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.Indexes
import com.sksamuel.elastic4s.http.{HttpClient, ElasticDsl}
import no.uio.musit.healthcheck.{HealthCheck, HealthCheckStatus, StopWatch}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class ElasticsearchHealthCheck @Inject()(
    implicit httpClient: HttpClient,
    ec: ExecutionContext
) extends HealthCheck {

  private[this] val indexes = Indexes(index.analysis.indexAlias, index.objects.indexAlias)
  private[this] val hcName  = "elasticsearch"

  override def healthCheck(): Future[HealthCheckStatus] = {
    // need to use import here due to name conflicts
    import ElasticDsl._

    val sw = StopWatch()
    httpClient
      .execute(search(indexes) query "*" size 0)
      .map { response =>
        if (response.totalHits > 0)
          HealthCheckStatus(
            name = hcName,
            available = true,
            responseTime = sw.elapsed(),
            message = Some(s"docs: ${response.totalHits} ")
          )
        else
          HealthCheckStatus(
            name = hcName,
            available = false,
            responseTime = sw.elapsed(),
            message = Some("No documents are indexed")
          )
      }
      .recover {
        case NonFatal(t) =>
          HealthCheckStatus(
            name = hcName,
            available = false,
            responseTime = sw.elapsed(),
            message = Some(t.getMessage)
          )
      }
  }

}
