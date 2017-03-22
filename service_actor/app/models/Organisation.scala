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

package models

import no.uio.musit.models.OrgId
import play.api.libs.json.{Json, Reads, Writes}

case class WordList(underlying: Seq[String]) {

  def asDbString = underlying.mkString("|", "|", "|")

}

object WordList {

  def fromDbString(str: String): WordList = {
    val words = str.stripPrefix("|").stripSuffix("|").split("\\|")
    WordList(words)
  }

  def fromOptDbString(mstr: Option[String]): Option[WordList] =
    mstr.map(WordList.fromDbString)

  implicit def wordListConverter(strSeq: Seq[String]): WordList = {
    WordList(strSeq)
  }

  implicit def wordListAsSeq(wl: WordList): Seq[String] = {
    wl.underlying
  }

  implicit val reads: Reads[WordList] = Reads { jsv =>
    jsv.validate[Seq[String]].map(WordList.apply)
  }

  implicit val writes: Writes[WordList] = Writes { wl =>
    Json.toJson(wl.underlying)
  }

}

/**
 * Domain Organization
 */
case class Organisation(
    id: Option[OrgId],
    fn: String,
    tel: String,
    web: String,
    synonyms: Option[WordList],
    serviceTags: Option[WordList]
)

object Organisation {
  val tupled          = (Organisation.apply _).tupled
  implicit val format = Json.format[Organisation]
}
