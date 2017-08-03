package repositories.analysis.dao

import com.google.inject.{Inject, Singleton}
import models.analysis._
import models.analysis.events.SampleCreated
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import no.uio.musit.repositories.events.EventActions
import no.uio.musit.models.ObjectTypes.SampleObjectType
import no.uio.musit.models._
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.shared.dao.SharedTables

import scala.concurrent.Future

@Singleton
class SampleObjectDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends AnalysisTables
    with AnalysisEventTableProvider
    with EventActions
    with AnalysisEventRowMappers
    with SharedTables {

  val logger = Logger(classOf[SampleObjectDao])

  import profile.api._

  def insert(
      so: SampleObject
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[ObjectUUID]] = {
    val soTuple = asSampleObjectTuple(so)
    val action  = sampleObjTable += soTuple

    db.run(action.transactionally)
      .map(_ => MusitSuccess(soTuple._1))
      .recover(nonFatal(s"An unexpected error occurred inserting a sample object"))
  }

  def insert(
      mid: MuseumId,
      so: SampleObject,
      eventObj: SampleCreated
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[ObjectUUID]] = {
    val soTuple = asSampleObjectTuple(so)

    insertAdditionalWithEvent(mid, eventObj)(asRow)(_ => sampleObjTable += soTuple)
      .map(_.map(_ => soTuple._1))
      .recover(nonFatal(s"An unexpected error occurred inserting a sample object"))
  }

  def update(
      so: SampleObject
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Unit]] = {
    val a = sampleObjTable.filter(_.id === so.objectId).update(asSampleObjectTuple(so))

    db.run(a.transactionally)
      .map {
        case res: Int if res == 1 => MusitSuccess(())
        case res: Int if 1 > res  => MusitDbError("Nothing was updated")
        case res: Int if 1 < res  => MusitDbError(s"Too many rows were updated: $res")
      }
      .recover(nonFatal(s"An unexpected error occurred updating sample ${so.sampleId}"))
  }

  def findByUUID(
      uuid: ObjectUUID
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[SampleObject]]] = {
    val q =
      sampleObjTable
        .filter(so => so.id === uuid && so.isDeleted === false)
        .result
        .headOption

    db.run(q)
      .map(sor => MusitSuccess(sor.map(fromSampleObjectRow)))
      .recover(nonFatal(s"An unexpected error occurred fetching sample object $uuid"))
  }

  def listForParentObject(
      parent: ObjectUUID
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Seq[SampleObject]]] = {
    val q = sampleObjTable.filter(_.parentId === parent).result

    db.run(q)
      .map(_.map(fromSampleObjectRow))
      .map(MusitSuccess.apply)
      .recover(
        nonFatal(s"An unexpected error occurred fetching child samples for $parent")
      )
  }

  def listForOriginatingObject(
      originating: ObjectUUID
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Seq[SampleObject]]] = {
    val q = sampleObjTable.filter(_.originatedFrom === originating).result

    db.run(q)
      .map(_.map(fromSampleObjectRow))
      .map(MusitSuccess.apply)
      .recover(
        nonFatal(
          s"An unexpected error occurred fetching samples for object $originating"
        )
      )
  }

  def listForMuseum(
      mid: MuseumId
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Seq[SampleObject]]] = {
    val q = sampleObjTable.filter(_.museumId === mid).result

    db.run(q)
      .map(_.map(fromSampleObjectRow))
      .map(MusitSuccess.apply)
      .recover(
        nonFatal(s"An unexpected error occurred fetching samples for Museum $mid")
      )
  }

  private def collectionFilter(
      collections: Seq[MuseumCollection]
  )(implicit currUsr: AuthenticatedUser) =
    if (currUsr.hasGodMode) ""
    else {
      val in = collections.map(_.collection.id).mkString("(", ",", ")")
      s"""AND mt.NEW_COLLECTION_ID in $in"""
    }

  // scalastyle:off method.length
  def listForNode(
      mid: MuseumId,
      nodeId: StorageNodeId,
      collections: Seq[MuseumCollection]
  )(
      implicit currUsr: AuthenticatedUser
  ): Future[MusitResult[Seq[EnrichedSampleObject]]] = {

    val query =
      sql"""
        SELECT /*+ FIRST_ROWS DRIVING_SITE(mt) */
          mt.MUSEUMNO, mt.SUBNO, mt.TERM,
          so.SAMPLE_UUID,
          so.ORIGINATED_OBJECT_UUID,
          so.PARENT_OBJECT_UUID,
          so.PARENT_OBJECT_TYPE,
          so.IS_EXTRACTED,
          so.MUSEUM_ID,
          so.STATUS,
          so.RESPONSIBLE_ACTOR,
          so.DONE_BY,
          so.DONE_DATE,
          so.SAMPLE_ID,
          so.SAMPLE_NUM,
          so.EXTERNAL_ID, so.EXTERNAL_ID_SOURCE,
          so.SAMPLE_TYPE_ID,
          so.SAMPLE_SIZE, so.SAMPLE_SIZE_UNIT,
          so.SAMPLE_CONTAINER,
          so.STORAGE_MEDIUM,
          so.TREATMENT,
          so.LEFTOVER_SAMPLE,
          so.DESCRIPTION,
          so.NOTE,
          so.REGISTERED_BY, so.REGISTERED_DATE,
          so.UPDATED_BY, so.UPDATED_DATE,
          so.IS_DELETED
        FROM
          MUSARK_ANALYSIS.SAMPLE_OBJECT so,
          MUSARK_STORAGE.NEW_LOCAL_OBJECT lo,
          MUSARK_STORAGE.NEW_EVENT ne,
          MUSIT_MAPPING.MUSITTHING mt
        WHERE so.MUSEUM_ID = ${mid.underlying}
          AND so.SAMPLE_UUID = ne.AFFECTED_UUID
          AND so.ORIGINATED_OBJECT_UUID = mt.MUSITTHING_UUID
          AND so.IS_DELETED = 0
          AND lo.CURRENT_LOCATION_ID = ${nodeId.asString}
          AND lo.OBJECT_TYPE = ${SampleObjectType.name}
          AND lo.LATEST_MOVE_ID = ne.EVENT_ID #${collectionFilter(collections)}
        ORDER BY
          mt.MUSEUMNOASNUMBER ASC,
          LOWER(mt.MUSEUMNO) ASC,
          mt.SUBNOASNUMBER ASC,
          LOWER(mt.SUBNO) ASC
        """.as[EnrichedSampleObject]

    db.run(query)
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"There was an error looking for samples for node $nodeId"))
  }

  // scalastyle:on method.length
}
