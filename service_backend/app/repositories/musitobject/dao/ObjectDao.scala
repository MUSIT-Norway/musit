package repositories.musitobject.dao

import com.google.inject.Inject
import models.musitobject._
import no.uio.musit.MusitResults._
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models.MuseumCollections._
import no.uio.musit.models._
import no.uio.musit.repositories.DbErrorHandlers
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import repositories.musitobject.dao.SearchFieldValues.{
  EmptyValue,
  FieldValue,
  LiteralValue,
  WildcardValue
}
import repositories.shared.dao.SharedTables

import scala.concurrent.{ExecutionContext, Future}

/**
 * Dao intended for searching through objects
 */
class ObjectDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext
) extends ObjectTables
    with SharedTables
    with DbErrorHandlers {

  val logger = Logger(classOf[ObjectDao])

  import profile.api._

  // Needs to be the same as Slicks no-escape-char value!
  // (Default second parameter value to the like function)
  val noEscapeChar = '\u0000'

  // Can be any char really.
  val escapeChar = '¤'

  type QObjectTable = Query[ObjectTable, ObjectTable#TableElementType, scala.Seq]

  /**
   * Since we build up a Slick Query object, we don't need to verify that the
   * rawValue is "safe", the database engine will validate the parameter value.
   * So security-wise, we don't need to guard against '--' etc.
   * We use '*' as wildcard symbol. And treat both '%' and '_' as ordinary
   * characters, both in like-tests and equality tests.
   *
   * @param rawValue and Option[String] with the value to classify
   * @return A classified instance of FieldValue
   */
  private[dao] def classifyValue(rawValue: Option[String]): Option[FieldValue] = {
    rawValue.map { raw =>
      if (raw.isEmpty) {
        EmptyValue()
      } else if (raw.contains('*')) {
        // Note that in the below expression, order is vital! It is essential that
        // the escapeChar -> escapeChar+escapeChar is done before the replacements
        // which introduces any escapeChars and that %->escapeChar happens
        // before *->'%'
        val wValue = raw
          .replace(escapeChar.toString, s"$escapeChar$escapeChar")
          .replace("%", s"$escapeChar%")
          .replace("_", s"${escapeChar}_")
          .replace('*', '%')

        val esc = if (wValue.contains(escapeChar)) escapeChar else noEscapeChar
        WildcardValue(wValue, esc)
      } else {
        LiteralValue(raw)
      }
    }
  }

  /**
   *
   * @param q
   * @param v
   * @tparam Q
   * @tparam C
   * @return
   */
  private def subNoFilter[Q <: QObjectTable, C](q: Q, v: FieldValue): QObjectTable = {
    v match {
      case EmptyValue() =>
        logger.debug("Using empty value for subNo filter")
        q
      case LiteralValue(value) =>
        logger.debug("Using literal value for subNo filter")
        q.filter(_.subNo.toUpperCase === value.toUpperCase)

      case WildcardValue(value, esc) =>
        logger.debug("Using wildcard value for subNo filter")
        q.filter(_.subNo.toUpperCase like (value.toUpperCase, esc))
    }
  }

  /**
   *
   * @param q
   * @param v
   * @tparam Q
   * @tparam C
   * @return
   */
  private def termFilter[Q <: QObjectTable, C](q: Q, v: FieldValue): QObjectTable = {
    v match {
      // No value to search for means we don't append a filter.
      case EmptyValue() =>
        logger.debug("Using empty value for term filter")
        q

      case LiteralValue(value) =>
        logger.debug("Using literal value for term filter")
        q.filter(_.term.toUpperCase === value.toUpperCase)

      case WildcardValue(value, esc) =>
        logger.debug("Using wildcard value for term filter")
        q.filter(_.term.toUpperCase like (value.toUpperCase, esc))
    }
  }

  /**
   *
   * @param q
   * @param v
   * @return
   */
  private def museumNoFilter(q: QObjectTable, v: FieldValue): QObjectTable = {
    v match {
      case EmptyValue() =>
        logger.debug("Using empty value for museumNo filter")
        q

      case LiteralValue(value) =>
        logger.debug("Using literal value for museumNo filter")
        val digitsOnly = value.forall(Character.isDigit)
        if (digitsOnly) q.filter(_.museumNoAsNumber === value.toLong)
        else q.filter(_.museumNo.toUpperCase === value.toUpperCase)

      case WildcardValue(value, esc) =>
        logger.debug("Using wildcard value for museumNo filter")
        q.filter(_.museumNo.toUpperCase like (value.toUpperCase, esc))
    }
  }

  /**
   *
   * @param mid
   * @param museumNo
   * @param subNo
   * @param term
   * @param collections
   * @param currUsr
   * @return
   */
  private[dao] def searchQuery(
      mid: MuseumId,
      museumNo: Option[MuseumNo],
      subNo: Option[SubNo],
      term: Option[String],
      collections: Seq[MuseumCollection]
  )(implicit currUsr: AuthenticatedUser): QObjectTable = {
    logger.debug(s"Performing search in collections: ${collections.mkString(", ")}")

    val mno = museumNo.map(_.value)

    val q1 = classifyValue(mno).map(f => museumNoFilter(objTable, f)).getOrElse(objTable)
    val q2 = classifyValue(subNo.map(_.value)).map(f => subNoFilter(q1, f)).getOrElse(q1)
    val q3 = classifyValue(term).map(f => termFilter(q2, f)).getOrElse(q2)
    val q4 = q3.filter(_.museumId === mid)
    val q5 = {
      if (currUsr.hasGodMode) q4
      // Filter on collection access if the user doesn't have GodMode
      else q4.filter(_.newCollectionId inSet collections.map(_.collection).distinct)
    }
    // Tweak here if sorting needs to be tuned
    q5.filter(_.isDeleted === false).sortBy { mt =>
      (
        mt.museumNoAsNumber.asc,
        mt.museumNo.toLowerCase.asc,
        mt.subNoAsNumber.asc,
        mt.subNo.toLowerCase.asc
      )
    }
  }

  /**
   * Searches the DB for objects based on 3 different criteria.
   *
   * @param mid
   * @param page
   * @param limit
   * @param museumNo
   * @param subNo
   * @param term
   * @param collections
   * @return
   */
  def search(
      mid: MuseumId,
      page: Int,
      limit: Int,
      museumNo: Option[MuseumNo],
      subNo: Option[SubNo],
      term: Option[String],
      collections: Seq[MuseumCollection]
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[ObjectSearchResult]] = {
    val offset = (page - 1) * limit
    val query  = searchQuery(mid, museumNo, subNo, term, collections)

    val totalMatches   = db.run(query.length.result)
    val matchedResults = db.run(query.drop(offset).take(limit).result)

    (for {
      total   <- totalMatches
      matches <- matchedResults
    } yield {
      MusitSuccess(
        ObjectSearchResult(total, matches.map(MusitObject.fromSearchTuple))
      )
    }).recover(nonFatal(s"Error while retrieving search result"))
  }

  /**
   *
   * @param mid
   * @param mainObjectId
   * @param collections
   * @param currUsr
   * @return
   */
  def findMainObjectChildren(
      mid: MuseumId,
      mainObjectId: ObjectUUID,
      collections: Seq[MuseumCollection]
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Seq[MusitObject]]] = {
    val colIds = collections.map(_.collection).distinct
    // scalastyle:off line.size.limit
    // format: off
    val query: DBIO[Seq[ObjectRow]] = for {
      maybeParent <- objTable.filter(_.uuid === mainObjectId).map(_.id).result.headOption
      children <- maybeParent.map { pid =>
        val q = objTable.filter(o => o.mainObjectId === pid.underlying && o.isDeleted === false)
        if (!currUsr.hasGodMode) q.filter(_.newCollectionId inSet colIds).result
        else q.result
      }.getOrElse(DBIO.successful(Vector.empty[ObjectRow]))
    } yield children
    // format: on
    // scalastyle:on line.size.limit

    db.run(query)
      .map(res => MusitSuccess(res.map(MusitObject.fromSearchTuple)))
      .recover(nonFatal(s"Error while retrieving search result"))
  }

  type QLocObj = Query[LocalObjectsTable, LocalObjectsTable#TableElementType, scala.Seq]

  private def collectionFilter(
      collections: Seq[MuseumCollection]
  )(implicit currUsr: AuthenticatedUser) =
    if (currUsr.hasGodMode) ""
    else {
      val in = collections.map(_.collection.id).mkString("(", ",", ")")
      s"""AND mt."NEW_COLLECTION_ID" in $in"""
    }

  private def pagingClause(page: Int, limit: Int): String = {
    val offset = (page - 1) * limit

    // MUSARK-787:
    // In Oracle, using bind variables for both OFFSET and FETCH NEXT in
    // prepared statements over DB links causes the DRIVING_SITE hint to be
    // dropped. An immediate, and significant, boost in performance is gained
    // when using "fixed" values for these.
    s"""OFFSET $offset ROWS FETCH NEXT $limit ROWS ONLY"""
  }

  /**
   * Count all objects in a node matching the given arguments.
   *
   * @param mid
   * @param nodeId
   * @param collections
   * @param currUsr
   * @return
   */
  private def countObjects(
      mid: MuseumId,
      nodeId: StorageNodeId,
      collections: Seq[MuseumCollection]
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Int]] = {
    val count =
      sql"""
        SELECT /*+DRIVING_SITE(mt)*/ COUNT(1)
        FROM "MUSARK_STORAGE"."NEW_LOCAL_OBJECT" lo, "MUSIT_MAPPING"."MUSITTHING" mt
        WHERE lo."MUSEUM_ID" = ${mid.underlying}
        AND lo."CURRENT_LOCATION_ID" = ${nodeId.asString}
        AND mt."MUSITTHING_UUID" = lo."OBJECT_UUID"
        AND mt."IS_DELETED" = 0 #${collectionFilter(collections)}
      """.as[Int].head

    db.run(count)
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"An error occurred counting objects in node $nodeId"))
  }

  // scalastyle:off line.size.limit method.length
  /**
   * Fetch all objects for the given arguments.
   *
   * You may notice that the below query doesn't have any ORDER BY clause.
   * This is intentional since executing an ORDER BY over a database link,
   * where the remote database is set to be the DRIVING_SITE, tend to be very
   * slow.
   *
   * Instead the result set is already sorted in the view being accessed
   * through the database link. That means we the result set we receive is
   * already sorted, and removes a heavy computational process when querying.
   *
   * @param mid
   * @param nodeId
   * @param collections
   * @param page
   * @param limit
   * @param currUsr
   * @return
   */
  private def objectsFor(
      mid: MuseumId,
      nodeId: StorageNodeId,
      collections: Seq[MuseumCollection],
      page: Int,
      limit: Int
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Seq[ObjectRow]]] = {
    // format: off
    val query =
      sql"""
        SELECT /*+ FIRST_ROWS DRIVING_SITE(mt) */ mt."OBJECT_ID",
          mt."MUSITTHING_UUID",
          mt."MUSEUMID",
          mt."MUSEUMNO",
          mt."MUSEUMNOASNUMBER",
          mt."SUBNO",
          mt."SUBNOASNUMBER",
          mt."MAINOBJECT_ID",
          mt."IS_DELETED",
          mt."TERM",
          mt."OLD_SCHEMANAME",
          mt."LOKAL_PK",
          mt."NEW_COLLECTION_ID",
          mt."ARK_FORM",
          mt."ARK_FUNN_NR",
          mt."NAT_STAGE",
          mt."NAT_GENDER",
          mt."NAT_LEGDATO",
          mt."NUM_DENOTATION",mt."NUM_VALOR",mt."NUM_DATE",mt."NUM_WEIGHT"
        FROM "MUSARK_STORAGE"."NEW_LOCAL_OBJECT" lo, "MUSIT_MAPPING"."MUSITTHING" mt
        WHERE lo."MUSEUM_ID" = ${mid.underlying}
        AND mt."MUSEUMID" = ${mid.underlying}
        AND lo."CURRENT_LOCATION_ID" = ${nodeId.asString}
        AND mt."MUSITTHING_UUID" = lo."OBJECT_UUID"
        AND mt."IS_DELETED" = 0 #${collectionFilter(collections)}
        ORDER BY
          mt."MUSEUMNOASNUMBER" ASC,
          LOWER(mt."MUSEUMNO") ASC,
          mt."SUBNOASNUMBER" ASC,
          LOWER(mt."SUBNO") ASC
        #${pagingClause(page, limit)}
      """.as[(Option[Long], Option[String], Int, String, Option[Long], Option[String], Option[Long], Option[Long], Boolean, String, Option[String], Option[Long], Option[Int],
        Option[String], Option[String], Option[String], Option[String], Option[String],(Option[String], Option[String], Option[String], Option[String]))]

    db.run(query).map { r =>
      val res = r.map { t =>
        (t._1.map(ObjectId.apply), t._2.map(ObjectUUID.unsafeFromString),
          MuseumId.fromInt(t._3), t._4, t._5, t._6, t._7, t._8, t._9, t._10,
          t._11, t._12, t._13.map(Collection.fromInt), t._14, t._15, t._16, t._17, t._18, t._19)
      }
      MusitSuccess(res)
    }
    // format:on
  }

  // scalastyle:on line.size.limit method.length

  /**
   * Fetch all objects matching the given criteria.
   *
   * @param mid
   * @param nodeId
   * @param collections
   * @param page
   * @param limit
   * @param currUsr
   * @return
   */
  def pagedObjects(
    mid: MuseumId,
    nodeId: StorageNodeId,
    collections: Seq[MuseumCollection],
    page: Int,
    limit: Int
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[PagedResult[MusitObject]]] =
    (for {
      tot <- MusitResultT(countObjects(mid, nodeId, collections))
      res <- MusitResultT(objectsFor(mid, nodeId, collections, page, limit))
    } yield {
      PagedResult[MusitObject](tot, res.map(MusitObject.fromSearchTuple))
    }).value.recover(nonFatal(s"Error while retrieving objects for nodeId $nodeId"))

  /**
   * Find the ObjectIds for objects located in the given old schema with the
   * provided old IDs.
   *
   * @param oldSchema
   * @param oldIds
   * @return
   */
  def findObjectIdsForOld(
    oldSchema: String,
    oldIds: Seq[Long]
  ): Future[MusitResult[Seq[ObjectId]]] = {
    val query = objTable.filter { o =>
      o.isDeleted === false &&
        o.oldSchema === oldSchema &&
        (o.oldObjId inSet oldIds)
    }.sortBy(_.id).map(_.id)

    db.run(query.result).map(MusitSuccess.apply)
      .recover(nonFatal(s"Error locating objectIds for old IDs ${oldIds.mkString(", ")}"))
  }

  /**
   * Find the object with the given old object ID kept in the given old schema.
   *
   * @param oldId
   * @param oldSchema
   * @return
   */
  def findByOldId(
    oldId: Long,
    oldSchema: String
  ): Future[MusitResult[Option[MusitObject]]] = {
    val query = objTable.filter { o =>
      o.oldObjId === oldId &&
        o.isDeleted === false &&
        o.oldSchema === oldSchema
    }

    db.run(query.result.headOption)
      .map(res => MusitSuccess(res.map(MusitObject.fromSearchTuple)))
      .recover(nonFatal(s"Error while locating object with old object ID $oldId"))
  }

  def findByUUID(
    museumId: MuseumId,
    objectUUID: ObjectUUID,
    collections: Seq[MuseumCollection]
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[MusitObject]]] = {
    val cids = collections.map(_.collection).distinct

    val queryAllCollections = objTable.filter { o =>
      o.uuid === objectUUID &&
        o.museumId === museumId &&
        o.isDeleted === false
    }

    val query =
      if (currUsr.hasGodMode) queryAllCollections
      else queryAllCollections.filter(_.newCollectionId inSet cids)

    db.run(query.result.headOption)
      .map(res => MusitSuccess(res.map(MusitObject.fromSearchTuple)))
      .recover(nonFatal(s"Error while locating object with uuid $objectUUID"))
  }

  def findByOldBarcode(
    museumId: MuseumId,
    oldBarcode: Long,
    collections: Seq[MuseumCollection]
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Seq[MusitObject]]] = {
    val cids = collections.map(_.collection).distinct

    val queryAllCollections = objTable.filter { o =>
      o.oldBarcode === oldBarcode &&
        o.museumId === museumId &&
        o.isDeleted === false
    }

    val query =
      if (currUsr.hasGodMode) queryAllCollections
      else queryAllCollections.filter(_.newCollectionId inSet cids)

    db.run(query.result)
      .map(res => MusitSuccess(res.map(MusitObject.fromSearchTuple)))
      .recover(nonFatal(s"Error while locating object with old barcode $oldBarcode"))

  }

  def getObjectMaterialAction(oid: ObjectId, collection: Collection)
    (implicit currUsr: AuthenticatedUser): DBIO[Seq[MusitObjectMaterial]] = {
    collection match {
      case Archeology =>
        thingMaterialTable.filter(_.objectid === oid.underlying).map { a =>
          (a.arkMaterial, a.arkSpesMaterial, a.arkSorting)
        }.result.map(ts => ts.map(t => ArkMaterial(t._1, t._2, t._3)))

      case Ethnography =>
        thingMaterialTable.filter(_.objectid === oid.underlying).map { e =>
          (e.etnMaterial, e.etnMaterialtype, e.etnMaterialElement)
        }.result.map(ts => ts.map(t => EtnoMaterial(t._1, t._2, t._3)))

      case Numismatics =>
        thingMaterialTable.filter(_.objectid === oid.underlying).map { n =>n.numMaterial
        }.result.map(ts => ts.map( t => NumMaterial(t)))

      case noMaterials =>
        logger.warn(s"There are no materials for the $noMaterials collection")
        DBIO.successful(Seq.empty)
    }
  }

  // case n: Collection with Nature => ???

  def getObjectMaterial(
    museumId: MuseumId,
    collection: Collection,
    oid: ObjectId
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Seq[MusitObjectMaterial]]] = {
    val q = getObjectMaterialAction(oid, collection)
    db.run(q)
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"Unable to get materials for $oid"))
  }

  def getObjectLocationAction(
    oid: ObjectId,
    collection: Collection
  )(implicit currUsr: AuthenticatedUser): DBIO[Seq[MusitObjectLocation]] = {
    collection match {
      case Archeology =>
        thingLocationTable.filter(_.objectid === oid.underlying).map { a =>
          (a.arkFarm, a.arkFarmNo, a.arkBrukNo)
        }.result.map(ts => ts.map(t => ArkLocation(t._1, t._2, t._3)))

      case nat: Collection with Nature =>
        thingLocationTable.filter(_.objectid === oid.underlying).map { n =>
          (
            n.natCountry,
            n.natStateProvince,
            n.natMunicipality,
            n.natLocality,
            n.natCoordinate,
            n.natCoordDatum,
            n.natSoneBand
          )
        }.result.map { ts =>
          ts.map(t => NatLocation(t._1, t._2, t._3, t._4, t._5, t._6, t._7))
        }
      case Ethnography =>
        thingLocationTable.filter(_.objectid === oid.underlying).map { e =>
          (e.etnPlace, e.etnCountry, e.etnRegion1, e.etnRegion2, e.etnArea)
        }.result.map(ts => ts.map(t => EtnoLocation(t._1, t._2, t._3, t._4, t._5)))

      case noLocations =>
        logger.warn(s"There are no locations for the $noLocations collection")
        DBIO.successful(Seq.empty)
    }
  }


  def getObjectCoordinateAction(
    oid: ObjectId,
    collection: Collection
  )(implicit currUsr: AuthenticatedUser): DBIO[Seq[MusitObjectCoordinate]] = {
    collection match {
      case Archeology =>
        thingCoordinateTable.filter(_.objectid === oid.underlying).map { a =>
          (a.arkProjection, a.arkPresision, a.arkNorth, a.arkEast)
        }.result.map(ts => ts.map(t => ArkCoordinate(t._1, t._2, t._3, t._4)))


      case noCoordinates =>
        logger.warn(s"There are no coordinates for the $noCoordinates collection")
        DBIO.successful(Seq.empty)
    }
  }

  def getObjectLocation(
    museumId: MuseumId,
    collection: Collection,
    oid: ObjectId
  )(implicit au: AuthenticatedUser): Future[MusitResult[Seq[MusitObjectLocation]]] = {
    val q = getObjectLocationAction(oid, collection)
    db.run(q)
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"Unable to get locations for $oid"))
  }

  def getObjectCoordinate(
    museumId: MuseumId,
    collection: Collection,
    oid: ObjectId
  )(implicit au: AuthenticatedUser): Future[MusitResult[Seq[MusitObjectCoordinate]]] = {
    val q = getObjectCoordinateAction(oid, collection)
    db.run(q)
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"Unable to get coordinates for $oid"))
  }

  def uuidsForIds(
    ids: Seq[ObjectId]
  ): Future[MusitResult[Seq[(ObjectId, ObjectUUID)]]] = {
    val query =
      sql"""
           SELECT t.OBJECT_ID, t.MUSITTHING_UUID FROM MUSIT_MAPPING.MUSITTHING t
           WHERE t.OBJECT_ID IN (#${ids.mkString(",")})
         """.as[(Long, String)]

    db.run(query)
      .map { res =>
        MusitSuccess(
          res.map(r => ObjectId.fromLong(r._1) -> ObjectUUID.unsafeFromString(r._2))
        )
      }
      .recover(nonFatal("An error occurred trying to fetch UUIDs for objects"))
  }

}
