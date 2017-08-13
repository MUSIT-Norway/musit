package repositories.elasticsearch.dao

import com.google.inject.{Inject, Singleton}
import models.musitobject.MusitObject
import no.uio.musit.models.ObjectId
import org.joda.time.DateTime
import org.reactivestreams.Publisher
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.musitobject.dao.ObjectTables
import slick.jdbc.{ResultSetConcurrency, ResultSetType}

import scala.concurrent.Future

@Singleton
class ElasticsearchThingsDao @Inject()(val dbConfigProvider: DatabaseConfigProvider)
    extends ObjectTables {

  import profile.api._

  /**
   * Implicit to enable slick to use number operations like >= and <= on ObjectId.
   */
  implicit def longToObjectId(n: Long): Rep[ObjectId] =
    LiteralColumn(ObjectId.fromLong(n))

  def objectStreams(
      streams: Int,
      fetchSize: Int,
      afterDate: Option[DateTime] = None
  ): Future[Seq[Publisher[MusitObject]]] = {
    val maxIdValue =
      objTable.filter(row => row.isDeleted === false && row.uuid.isDefined).map(_.id).max

    db.run(maxIdValue.result).map { maxId =>
      ElasticsearchThingsDao.indexRanges(streams, maxId.get.underlying).map {
        case (from, to) =>
          val query = objTable.filter { row =>
            (row.isDeleted === false) && (row.id >= from) && (row.id <= to)
          }
          db.stream(
              query.result
                .withStatementParameters(
                  rsType = ResultSetType.ForwardOnly,
                  rsConcurrency = ResultSetConcurrency.ReadOnly,
                  fetchSize = fetchSize
                )
                .transactionally
            )
            .mapResult(MusitObject.fromSearchTuple)
      }
    }
  }

  def objectsChangedAfterTimstampStream(
      fetchSize: Int,
      afterDate: DateTime
  ): Publisher[MusitObject] = {
    val query = objTable.filter { row =>
      (row.isDeleted === false) && row.updatedDate > afterDate
    }

    db.stream(
        query.result
          .withStatementParameters(
            rsType = ResultSetType.ForwardOnly,
            rsConcurrency = ResultSetConcurrency.ReadOnly,
            fetchSize = fetchSize
          )
          .transactionally
      )
      .mapResult(MusitObject.fromSearchTuple)
  }

}

object ElasticsearchThingsDao {
  def indexRanges(count: Int, lastId: Long): List[(Long, Long)] = {
    val oi = Range.Long(lastId / count, lastId + 1, (lastId + 1) / count)
    oi.foldLeft(List.empty[(Long, Long)]) {
        case (list, id) =>
          if (list.isEmpty) (0l, id) :: Nil
          else (list.head._2 + 1, id) :: list
      }
      .reverse
  }

}
