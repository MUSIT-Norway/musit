package repositories.elasticsearch.dao

import com.google.inject.{Inject, Singleton}
import models.musitobject.MusitObject
import no.uio.musit.models.ObjectId
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.musitobject.dao.ObjectTables
import slick.basic.DatabasePublisher
import slick.jdbc.{ResultSetConcurrency, ResultSetType}

import scala.concurrent.Future

@Singleton
class ElasticsearchThingsDao @Inject()(val dbConfigProvider: DatabaseConfigProvider)
    extends ObjectTables {

  import profile.api._

  implicit def longToObjectId(n: Long): Rep[ObjectId] =
    LiteralColumn(ObjectId.fromLong(n))

  def objectStreams(streams: Int): Future[Seq[DatabasePublisher[MusitObject]]] = {

    val maxIdValue =
      objTable.filter(row => row.isDeleted === false && row.uuid.isDefined).map(_.id).max

    db.run(maxIdValue.result).map { maxId =>
      indexRanges(streams, maxId.get.underlying).map {
        case (from, to) =>
          val query = objTable.filter { row =>
            (row.isDeleted === false) && (row.id >= from) && (row.id <= to)
          }
          db.stream(
              query.result
                .withStatementParameters(
                  rsType = ResultSetType.ForwardOnly,
                  rsConcurrency = ResultSetConcurrency.ReadOnly,
                  fetchSize = 1000
                )
                .transactionally
            )
            .mapResult(row => MusitObject.fromSearchTuple(row))
      }
    }
  }

  def indexRanges(count: Int, max: Long): Seq[(Long, Long)] = {
    val oi = Range.Long(max / count, max + 1, (max + 1) / count)
    val zi = Range.Long(0, max, max / count).map(_ + 1)
    zi.zip(oi)
  }

}
