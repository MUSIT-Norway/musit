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
import no.uio.musit.models.{MuseumCollection, MuseumId, MuseumNo, SubNo}
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.service.MusitResults._
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * Dao intended for searching through objects
 */
class ObjectSearchDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends ObjectTables {

  val logger = Logger(classOf[ObjectAggregationDao])

  import driver.api._

  private val table = TableQuery[ObjectTable]

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

  private def subNoFilter[Q <: QObjectTable, C](
    q: Q,
    value: FieldValue
  ): QObjectTable = {
    value match {
      case EmptyValue() =>
        logger.debug("Using empty value for subNo filter")
        q
      case LiteralValue(v) =>
        logger.debug("Using literal value for subNo filter")
        q.filter(_.subNo.toUpperCase === v.toUpperCase)

      case WildcardValue(v, esc) =>
        logger.debug("Using wildcard value for subNo filter")
        q.filter(_.subNo.toUpperCase like (v.toUpperCase, esc))
    }
  }

  private def termFilter[Q <: QObjectTable, C](
    q: Q,
    value: FieldValue
  ): QObjectTable = {
    value match {
      // No value to search for means we don't append a filter.
      case EmptyValue() =>
        logger.debug("Using empty value for term filter")
        q

      case LiteralValue(v) =>
        logger.debug("Using literal value for term filter")
        q.filter(_.term.toUpperCase === v.toUpperCase)

      case WildcardValue(v, esc) =>
        logger.debug("Using wildcard value for term filter")
        q.filter(_.term.toUpperCase like (v.toUpperCase, esc))
    }
  }

  private def museumNoFilter(q: QObjectTable, value: FieldValue): QObjectTable = {
    value match {
      case EmptyValue() =>
        logger.debug("Using empty value for museumNo filter")
        q

      case LiteralValue(v) =>
        logger.debug("Using literal value for museumNo filter")
        val digitsOnly = v.forall(Character.isDigit)
        if (digitsOnly) q.filter(_.museumNoAsNumber === v.toLong)
        else q.filter(_.museumNo.toUpperCase === v.toUpperCase)

      case WildcardValue(v, esc) =>
        logger.debug("Using wildcard value for museumNo filter")
        q.filter(_.museumNo.toUpperCase like (v.toUpperCase, esc))
    }
  }

  private[dao] def searchQuery(
    mid: MuseumId,
    page: Int,
    pageSize: Int,
    museumNo: Option[MuseumNo],
    subNo: Option[SubNo],
    term: Option[String],
    collections: Seq[MuseumCollection]
  )(implicit currUsr: AuthenticatedUser): QObjectTable = {
    logger.debug(s"Performing search in collections: ${collections.mkString(", ")}")

    val mno = museumNo.map(_.value)

    val q1 = classifyValue(mno).map(f => museumNoFilter(table, f)).getOrElse(table)
    val q2 = classifyValue(subNo.map(_.value)).map(f => subNoFilter(q1, f)).getOrElse(q1)
    val q3 = classifyValue(term).map(f => termFilter(q2, f)).getOrElse(q2)
    val q4 = q3.filter(_.museumId === mid)
    val q5 = {
      if (currUsr.hasGodMode) q4
      // Filter on collection access if the user doesn't have GodMode
      else q4.filter(_.oldSchema inSet collections.flatMap(_.flattenSchemas).distinct)
    }
    // Tweak here if sorting needs to be tuned
    q5.sortBy { mt =>
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
   * @param pageSize
   * @param museumNo
   * @param subNo
   * @param term
   * @param collections
   * @return
   */
  def search(
    mid: MuseumId,
    page: Int,
    pageSize: Int,
    museumNo: Option[MuseumNo],
    subNo: Option[SubNo],
    term: Option[String],
    collections: Seq[MuseumCollection]
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[ObjectSearchResult]] = {
    val offset = (page - 1) * pageSize
    val query = searchQuery(mid, page, pageSize, museumNo, subNo, term, collections)

    val totalMatches = db.run(query.length.result)
    val matchedResults = db.run(query.drop(offset).take(pageSize).result)

    (for {
      total <- totalMatches
      matches <- matchedResults
    } yield {
      logger.debug(s"Gpt ")
      MusitSuccess(
        ObjectSearchResult(total, matches.map(MusitObject.fromTuple))
      )
    }).recover {
      case e: Exception =>
        val msg = s"Error while retrieving search result"
        logger.error(msg, e)
        MusitDbError(msg, Some(e))
    }
  }

}