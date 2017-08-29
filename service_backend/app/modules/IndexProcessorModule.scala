package modules

import net.codingwell.scalaguice.ScalaModule
import services.elasticsearch.index.ElasticsearchService

class IndexProcessorModule extends ScalaModule {

  override def configure() = {
    bind[ElasticsearchService].asEagerSingleton()
  }

}
