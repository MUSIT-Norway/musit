import no.uio.musit.models._
import no.uio.musit.security.AuthenticatedUser
import play.api.libs.json.Json
import play.api.mvc.{Result, Results}

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

package object controllers {

  type SimpleNode = (StorageNodeDatabaseId, String)

  /**
   * TODO: This is quite dirty... should be improved when time allows.
   *
   * @param mid
   * @param str
   * @param currUsr
   * @return
   */
  def parseCollectionIdsParam(
    mid: MuseumId,
    str: String
  )(implicit currUsr: AuthenticatedUser): Either[Result, Seq[MuseumCollection]] = {
    val colIds = str.split(",").map { ss =>
      val s = ss.trim
      (s, CollectionUUID.fromString(s))
    }.toSeq
    val badCols = colIds.filter(_._2.isEmpty)

    if (str.isEmpty) {
      Left(Results.BadRequest(Json.obj(
        "message" -> s"collectionIds param must contain at least 1 collection ID."
      )))
    } else if (badCols.nonEmpty) {
      Left(Results.BadRequest(Json.obj(
        "message" -> s"Invalid CollectionUUIDs: ${badCols.map(_._1).mkString(", ")}"
      )))
    } else {
      val cids = colIds.filter(_._2.nonEmpty)
        .flatMap(_._2)
        .filter(cid => currUsr.canAccess(mid, cid))

      val cols = currUsr.collectionsFor(mid).filter(mc => cids.contains(mc.uuid))

      if (cols.size < colIds.size && !currUsr.hasGodMode) {
        Left(Results.Forbidden(
          Json.obj("message" -> (s"Not authorized to access one or more of " +
            s" the following collection(s) $str"))
        ))
      } else {
        Right(cols)
      }
    }
  }

}
