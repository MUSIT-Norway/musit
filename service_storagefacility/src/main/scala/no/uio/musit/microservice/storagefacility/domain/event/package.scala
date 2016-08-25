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

package no.uio.musit.microservice.storagefacility.domain

import play.api.libs.json._

package object event {

  type EventProps = Map[String, Any]

  object Implicits {

    /**
     * Implicit JSON transformers for Map[String, Any].
     */
    implicit object MapStrAnyFormats extends Format[EventProps] {
      def writes(m: EventProps) =
        JsObject(m.mapValues {
          case str: String => Json.toJson[String](str)
          case num: Int => Json.toJson[Int](num)
          case num: Short => Json.toJson[Short](num)
          case num: Long => Json.toJson[Long](num)
          case num: Double => Json.toJson[Double](num)
          case num: Float => Json.toJson[Float](num)
          case boo: Boolean => Json.toJson[Boolean](boo)
          case js: JsValue => js
          case x => Json.toJson[String](x.toString)
        })

      /**
       * This, rather naïve, reads should have more validation to ensure the
       * value types are within the supported set of types. And if not, a JsError
       * should be returned.
       */
      def reads(json: JsValue): JsResult[EventProps] = {
        val tmpMap = json.as[Map[String, JsValue]]
        JsSuccess(tmpMap.mapValues {
          case JsString(str) => str
          case JsNumber(num) => num
          case JsBoolean(bool) => bool
          case value: JsValue => value.toString()
        })
      }
    }

  }

}
