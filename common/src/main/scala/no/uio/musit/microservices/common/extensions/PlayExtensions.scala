/*
 *   MUSIT is a cooperation between the university museums of Norway.
 *   Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License,
 *   or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License along
 *   with this program; if not, write to the Free Software Foundation, Inc.,
 *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package no.uio.musit.microservices.common.extensions

import java.net.URI

import play.api.libs.ws.{ WSRequest, WSResponse }
import play.api.mvc.Request

//import play.mvc.results._

import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
//Not sure about whether it is a good practice to use this

/**
 * Created by jstabel on 4/15/16.
 */

object PlayExtensions {

  // TODO: Use official Play exception classes or something
  case class MusitHttpErrorInfo(val uri: Option[URI], status: Int, serverMsg: String, localMsg: String)

  class MusitHttpError(val info: MusitHttpErrorInfo) extends Exception(info.localMsg)

  class MusitBadRequest(info: MusitHttpErrorInfo) extends MusitHttpError(info)

  class MusitAuthFailed(info: MusitHttpErrorInfo) extends MusitHttpError(info)

  // TODO: Use another exception class when the above todo-item has been done
  def newAuthFailed(msg: String) = new MusitAuthFailed(MusitHttpErrorInfo(None, 401, "", msg))

  def throwAuthFailed(msg: String) = throw newAuthFailed(msg)

  implicit class RequestImp[T](val req: Request[T]) extends AnyVal {

    ///Gets the value of the Bearer token in the Authorization header, if any.
    def getBearerToken: Option[String] = {
      val authHeader = req.headers.getAll("Authorization")
      val res = authHeader.find(s => s.startsWith("Bearer ")) //We include the space because we don't want to get anything "accidentally" starting with the letters "Bearer"
      res.map(b => b.substring("Bearer ".length)) //Remove the "Bearer " start of the string
        .map(_.trim) //Probably not necessary to trim the rest, but it may be convenient if the sender has accidentally sent in blanks
    }
  }

  implicit class WSRequestImp(val wsr: WSRequest) extends AnyVal {
    def withBearerToken(token: String) = {
      wsr.withHeaders("Authorization" -> ("Bearer " + token))
    }

    ///Gets the value of the Bearer token in the Authorization header, if any.
    def getBearerToken: Option[String] = {
      wsr.headers.get("Authorization").flatMap(_.find(s => s.startsWith("Bearer "))) //We include the space because we don't want to get anything "accidentally" starting with the letters "Bearer"
        .map(b => b.substring("Bearer ".length)) //Remove the "Bearer " start of the string
        .map(_.trim) //Probably not necessary to trim the rest, but it may be convenient if the sender has accidentally sent in blanks
    }

    def postJsonString(text: String): Future[WSResponse] = {
      //println(s"text to parse: $text")
      val json = Json.parse(text)
      //println(s"Parsed json: $json")
      val res = wsr.post(json)
      //res.map(resp => println(s"result body: ${resp.body}"))
      res
    }

    def putJsonString(text: String): Future[WSResponse] = {
      //println(s"text to parse: $text")
      val json = Json.parse(text)
      //println(s"Parsed json: $json")
      val res = wsr.put(json)
      //res.map(resp => println(s"result body: ${resp.body}"))
      res
    }

    /*def putJsValue(value: Jsvalue) : Future[WSResponse] = {
      //println(s"text to parse: $text")
      val json = Json.parse(text)
      //println(s"Parsed json: $json")
      val res = wsr.withHeaders("Content-Type"->"application/json")
      res.map(resp => println(s"result body: ${resp.body}"))
      res
    }*/

    // TODO: Handle more exceptions
    def translateStatusToException(resp: WSResponse) = {
      assert(resp.status < 200 || resp.status >= 300)
      val msg = s"Unable to access http resource: ${wsr.uri} Status: ${resp.status} StatusText: ${resp.statusText}"
      resp.status match {
        case 400 => new MusitBadRequest(new MusitHttpErrorInfo(Some(wsr.uri), resp.status, resp.statusText, msg))
        case 401 => new MusitAuthFailed(new MusitHttpErrorInfo(Some(wsr.uri), resp.status, resp.statusText, msg))
        case _ => new MusitHttpError(new MusitHttpErrorInfo(Some(wsr.uri), resp.status, resp.statusText, msg))
      }
    }

    def getOrFail() = {

      val respF = wsr.get()
      respF.flatMap { resp: WSResponse =>
        if (resp.status < 200 || resp.status >= 300) {
          Future.failed(translateStatusToException(resp))
        } else
          Future.successful(resp)
      }
    }
  }

}
