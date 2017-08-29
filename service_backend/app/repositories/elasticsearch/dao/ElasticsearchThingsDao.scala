package repositories.elasticsearch.dao

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.{Inject, Singleton}
import models.analysis.SampleObject
import models.musitobject.MusitObject
import no.uio.musit.models.ObjectId
import org.joda.time.DateTime
import play.api.db.slick.DatabaseConfigProvider
import repositories.analysis.dao.AnalysisTables
import repositories.musitobject.dao.ObjectTables
import slick.jdbc.{ResultSetConcurrency, ResultSetType}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ElasticsearchThingsDao @Inject()(
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

  def objectStreams(
      streams: Int,
      fetchSize: Int
  ): Future[Seq[Source[MusitObject, NotUsed]]] = {
    val maxIdValue =
      objTable.filter(_.uuid.isDefined).map(_.id).max

    db.run(maxIdValue.result).map { maxId =>
      ElasticsearchThingsDao.indexRanges(streams, maxId.get.underlying).map {
        case (from, to) =>
          val query = objTable.filter(row => (row.id >= from) && (row.id <= to))
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
