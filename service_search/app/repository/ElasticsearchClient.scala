/*
 *  MUSIT is a museum database to archive natural and cultural history data.
 *  Copyright (C) 2017  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License,
 *  or any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package repository

import com.google.inject.Inject
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import play.api.http.{ContentTypes, HeaderNames, Status}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}

import scala.concurrent.Future

class ElasticsearchClient @Inject() (cfg: Configuration, ws: WSClient) {

  val logger = Logger(classOf[ElasticsearchClient])
  private val baseUrl = cfg.getString("musit.elasticsearch.url")
    .getOrElse("http://localhost:9200")

  def client(parts: String*) = {
    val path = s"$baseUrl/${parts.mkString("/")}"
    logger.info(s"calling path $path parts: $parts")
    ws
      .url(path)
      .withHeaders(HeaderNames.ACCEPT -> ContentTypes.JSON)
      .withHeaders(HeaderNames.CONTENT_TYPE -> ContentTypes.JSON)
  }

  def insertDocument(
    index: String,
    documentType: String,
    id: String,
    doc: JsValue,
    refresh: Boolean = false
  ): Future[MusitResult[JsValue]] = {
    client(index, documentType, id)
      .withQueryString("refresh" -> refresh.toString)
      .put(doc)
      .map(r => r.status match {
        case Status.OK => MusitSuccess(r.json)
        case Status.CREATED => MusitSuccess(r.json)
        case httpCode => MusitDbError(s"Unexpected return code $httpCode")
      })
  }

  def getDocument(
    index: String,
    documentType: String,
    id: String
  ): Future[MusitResult[Option[JsValue]]] = {
    client(index, documentType, id)
      .get()
      .map(r => r.status match {
        case Status.OK => MusitSuccess(Some(r.json))
        case Status.NOT_FOUND => MusitSuccess(None)
        case httpCode => MusitDbError(s"Unexpected return code $httpCode")
      })
  }

  def doSearch(
    query: String,
    index: Option[String],
    documentType: Option[String]
  ): Future[MusitResult[JsValue]] = {
    val parts = index.map {
      List(_) ++ documentType.map(List(_)).getOrElse(Nil)
    }.getOrElse(Nil) :+ "_search"
    client(parts: _*)
      .withQueryString("q" -> query)
      .get()
      .map(r => r.status match {
        case Status.OK => MusitSuccess(r.json)
        case httpCode => MusitDbError(s"Unexpected return code $httpCode")
      })
  }

}
