/*
 * MUSIT is a museum database to archive natural and cultural history data.
 * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package repositories.dao

import com.google.inject.Inject
import models.SearchFieldValues._
import models.{MusitObject, ObjectSearchResult}
import no.uio.musit.MusitResults._
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.models._
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.control.NonFatal

/**
 * Dao intended for searching through objects
 */
class ObjectDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends Tables {

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
      else q4.filter(_.newCollectionId inSet collections.flatMap(_.schemaIds).distinct)
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
        ObjectSearchResult(total, matches.map(MusitObject.fromTuple))
      )
    }).recover {
      case NonFatal(ex) =>
        val msg = s"Error while retrieving search result"
        logger.error(msg, ex)
        MusitDbError(msg, Some(ex))
    }
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
      mainObjectId: ObjectId,
      collections: Seq[MuseumCollection]
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Seq[MusitObject]]] = {
    val q = objTable.filter { o =>
      o.mainObjectId === mainObjectId.underlying && o.isDeleted === false
    }
    val query = {
      if (currUsr.hasGodMode) q
      else q.filter(_.newCollectionId inSet collections.flatMap(_.schemaIds).distinct)
    }
    db.run(query.result)
      .map(res => MusitSuccess(res.map(MusitObject.fromTuple)))
      .recover {
        case NonFatal(ex) =>
          val msg = s"Error while retrieving search result"
          logger.error(msg, ex)
          MusitDbError(msg, Option(ex))
      }
  }

  type QLocObj = Query[LocalObjectsTable, LocalObjectsTable#TableElementType, scala.Seq]

  private def collectionFilter(
      collections: Seq[MuseumCollection]
  )(implicit currUsr: AuthenticatedUser) =
    if (currUsr.hasGodMode) ""
    else {
      val in = collections.flatMap(_.schemaIds).mkString("(", ",", ")")
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
      nodeId: StorageNodeDatabaseId,
      collections: Seq[MuseumCollection]
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Int]] = {
    val count =
      sql"""
        SELECT /*+DRIVING_SITE(mt)*/ COUNT(1)
        FROM "MUSARK_STORAGE"."LOCAL_OBJECT" lo, "MUSIT_MAPPING"."MUSITTHING" mt
        WHERE lo."MUSEUM_ID" = ${mid.underlying}
        AND lo."CURRENT_LOCATION_ID" = ${nodeId.underlying}
        AND mt."OBJECT_ID" = lo."OBJECT_ID"
        AND mt."IS_DELETED" = 0 #${collectionFilter(collections)}
      """.as[Int].head

    db.run(count).map(MusitSuccess.apply).recover {
      case NonFatal(ex) =>
        val msg = s"An error occurred counting objects in node $nodeId"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
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
      nodeId: StorageNodeDatabaseId,
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
          mt."NEW_COLLECTION_ID"
        FROM "MUSARK_STORAGE"."LOCAL_OBJECT" lo, "MUSIT_MAPPING"."MUSITTHING" mt
        WHERE lo."MUSEUM_ID" = ${mid.underlying}
        AND mt."MUSEUMID" = ${mid.underlying}
        AND lo."CURRENT_LOCATION_ID" = ${nodeId.underlying}
        AND mt."OBJECT_ID" = lo."OBJECT_ID"
        AND mt."IS_DELETED" = 0 #${collectionFilter(collections)}
        ORDER BY
          mt."MUSEUMNOASNUMBER" ASC,
          LOWER(mt."MUSEUMNO") ASC,
          mt."SUBNOASNUMBER" ASC,
          LOWER(mt."SUBNO") ASC
        #${pagingClause(page, limit)}
      """.as[(Option[Long], Option[String], Int, String, Option[Long], Option[String], Option[Long], Option[Long], Boolean, String, Option[String], Option[Long], Option[Int])]

    db.run(query).map { r =>
      val res = r.map { t =>
        (t._1.map(ObjectId.apply), t._2.map(ObjectUUID.unsafeFromString),
          MuseumId.fromInt(t._3), t._4, t._5, t._6, t._7, t._8, t._9, t._10,
          t._11, t._12, t._13)
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
      nodeId: StorageNodeDatabaseId,
      collections: Seq[MuseumCollection],
      page: Int,
      limit: Int
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[PagedResult[MusitObject]]] =
    (for {
      tot <- MusitResultT(countObjects(mid, nodeId, collections))
      res <- MusitResultT(objectsFor(mid, nodeId, collections, page, limit))
    } yield {
      PagedResult[MusitObject](tot, res.map(MusitObject.fromTuple))
    }).value.recover {
      case NonFatal(ex) =>
        val msg = s"Error while retrieving objects for nodeId $nodeId"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }

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

    db.run(query.result).map(MusitSuccess.apply).recover {
      case NonFatal(ex) =>
        val msg = s"Error locating objectIds for old IDs ${oldIds.mkString(", ")}"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
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
      .map(res => MusitSuccess(res.map(MusitObject.fromTuple)))
      .recover {
        case NonFatal(ex) =>
          val msg = s"Error while locating object with old object ID $oldId"
          logger.error(msg, ex)
          MusitDbError(msg, Option(ex))
      }
  }

  def findByUUID(
    museumId: MuseumId,
    objectUUID: ObjectUUID,
    collections: Seq[MuseumCollection]
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[MusitObject]]] = {
    val cids = collections.flatMap(_.schemaIds).distinct

    val queryAllCollections = objTable.filter { o =>
      o.uuid === objectUUID &&
      o.museumId === museumId &&
      o.isDeleted === false
    }

    val query =
      if (currUsr.hasGodMode) queryAllCollections
      else queryAllCollections.filter(_.newCollectionId inSet cids)

    db.run(query.result.headOption)
      .map(res => MusitSuccess(res.map(MusitObject.fromTuple)))
      .recover {
        case NonFatal(ex) =>
          val msg = s"Error while locating object with uuid $objectUUID"
          logger.error(msg, ex)
          MusitDbError(msg, Option(ex))
      }
  }

  def findByOldBarcode(
      museumId: MuseumId,
      oldBarcode: Long,
      collections: Seq[MuseumCollection]
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Seq[MusitObject]]] = {
    val cids = collections.flatMap(_.schemaIds).distinct

    val queryAllCollections = objTable.filter { o =>
      o.oldBarcode === oldBarcode &&
      o.museumId === museumId &&
      o.isDeleted === false
    }

    val query =
      if (currUsr.hasGodMode) queryAllCollections
      else queryAllCollections.filter(_.newCollectionId inSet cids)

    db.run(query.result)
      .map(res => MusitSuccess(res.map(MusitObject.fromTuple)))
      .recover {
        case NonFatal(ex) =>
          val msg = s"Error while locating object with old barcode $oldBarcode"
          logger.error(msg, ex)
          MusitDbError(msg, Option(ex))
      }
  }
}
