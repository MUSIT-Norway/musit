package modules

import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.HttpClient
import net.codingwell.scalaguice.ScalaModule
import no.uio.musit.ws.ProxiedRequest
import org.apache.http.HttpHost
import org.apache.http.client.config.RequestConfig
import org.elasticsearch.client.RestClientBuilder.RequestConfigCallback
import play.api.{Configuration, Environment}
import services.elasticsearch.ElasticsearchService

class ElasticsearchModule(environment: Environment, cfg: Configuration)
    extends ScalaModule {

  override def configure() = {
    val host = cfg.underlying.getString("musit.elasticsearch.host")
    val port = cfg.underlying.getInt("musit.elasticsearch.port")

    bind[ElasticsearchService].asEagerSingleton()

    bind[HttpClient].toInstance(
      HttpClient(
        ElasticsearchClientUri(host, port),
        new RequestConfigCallback {
          override def customizeRequestConfig(builder: RequestConfig.Builder) = {
            ProxiedRequest
              .readConfig(cfg)
              .map { p =>
                builder
                  .setProxy(
                    new HttpHost(
                      p.host,
                      p.port,
                      p.protocol.getOrElse(HttpHost.DEFAULT_SCHEME_NAME)
                    )
                  )
                  .setAuthenticationEnabled(false)
              }
              .getOrElse(builder)
          }
        }
      )
    )

  }

}
