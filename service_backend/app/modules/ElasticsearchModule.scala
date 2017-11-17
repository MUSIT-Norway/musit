package modules

import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.HttpClient
import net.codingwell.scalaguice.ScalaModule
import play.api.{Configuration, Environment}

class ElasticsearchModule(environment: Environment, cfg: Configuration)
    extends ScalaModule {

  override def configure() = {
    val host = cfg.underlying.getString("musit.elasticsearch.host")
    val port = cfg.underlying.getInt("musit.elasticsearch.port")

    bind[HttpClient].toInstance(HttpClient(ElasticsearchClientUri(host, port)))
  }

}
