package modules

import net.codingwell.scalaguice.ScalaModule
import services.elasticsearch.index.ElasticsearchIndexService

class IndexProcessorModule extends ScalaModule {

  override def configure() = {
    bind[ElasticsearchIndexService].asEagerSingleton()
  }

}
