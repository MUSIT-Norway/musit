package repositories.elasticsearch.dao

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.{Inject, Singleton}
import models.analysis.SampleObject
import models.musitobject.MusitObject
import no.uio.musit.models.MuseumCollections.Collection
import no.uio.musit.models.{MuseumId, ObjectId, ObjectUUID}
import org.joda.time.DateTime
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import repositories.analysis.dao.AnalysisTables
import repositories.musitobject.dao.ObjectTables
import slick.jdbc.{ResultSetConcurrency, ResultSetType}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ElasticsearchObjectsDao @Inject()(
    implicit val dbConfigProvider: DatabaseConfigProvider,
    ec: ExecutionContext
) extends ObjectTables
    with AnalysisTables {

  import profile.api._

  /**
   * Implicit to enable slick to use number operations like >= and <= on ObjectId.
   */
  implicit def longToObjectId(n: Long): Rep[ObjectId] =
    LiteralColumn(ObjectId.fromLong(n))

  private[this] val logger = Logger(classOf[ElasticsearchObjectsDao])

  def objectStreams(
      streams: Int,
      fetchSize: Int
  ): Future[Seq[Source[MusitObject, NotUsed]]] = {
    val maxIdValue =
      objTable.filter(_.uuid.isDefined).map(_.id).max

    db.run(maxIdValue.result).map { maxId =>
      ElasticsearchObjectsDao.indexRanges(streams, maxId.get.underlying).map {
        case (from, to) =>
          val query = objTable.filter(row => (row.id >= from) && (row.id <= to))
          Source.fromPublisher {

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
  }

  def objectsChangedAfterTimestampStream(
      fetchSize: Int,
      afterDate: DateTime
  ): Source[MusitObject, NotUsed] = {
    val query = objTable.filter(_.updatedDate > afterDate)

    Source.fromPublisher(
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
    )
  }

  def sampleStream(
      fetchSize: Int,
      afterTimestamp: Option[DateTime]
  ): Source[SampleObject, NotUsed] = {
    val query =
      afterTimestamp.map { after =>
        sampleObjTable.filter(
          r => r.registeredDate > after || r.updatedDate > after
        )
      }.getOrElse(sampleObjTable)

    Source.fromPublisher(
      db.stream(
          query.result
            .withStatementParameters(
              rsType = ResultSetType.ForwardOnly,
              rsConcurrency = ResultSetConcurrency.ReadOnly,
              fetchSize = fetchSize
            )
            .transactionally
        )
        .mapResult(fromSampleObjectRow)
    )
  }

  def findObjectsMidAndCollection(
      objects: Set[ObjectUUID]
  ): Future[Seq[(ObjectUUID, MuseumId, Collection)]] = {
    val query = objTable
      .filter(r => r.uuid.inSet(objects) && r.newCollectionId.isDefined)
      .map(r => (r.uuid.get, r.museumId, r.newCollectionId.get)) // safe to call get since all non null values are filtered out
    db.run(query.result)
  }

}

object ElasticsearchObjectsDao {
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
