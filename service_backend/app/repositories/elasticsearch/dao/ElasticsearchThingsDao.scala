package repositories.elasticsearch.dao

import com.google.inject.{Inject, Singleton}
import models.musitobject.MusitObject
import play.api.db.slick.DatabaseConfigProvider
import repositories.musitobject.dao.ObjectTables
import slick.basic.DatabasePublisher

@Singleton
class ElasticsearchThingsDao @Inject()(val dbConfigProvider: DatabaseConfigProvider)
    extends ObjectTables {

  import profile.api._

  def objectStream: DatabasePublisher[MusitObject] =
    db.stream(objTable.result).mapResult(row => MusitObject.fromSearchTuple(row))

}
