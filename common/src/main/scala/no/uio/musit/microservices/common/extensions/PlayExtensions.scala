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

import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.mvc.Results._

//import play.mvc.results._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

//Not sure about whether it is a good practice to use this

/**
  * Created by jstabel on 4/15/16.
  */


object PlayExtensions {

  // TODO: Use official Play exception classes
  case class MusitHttpErrorInfo(val uri: Option[URI], status: Int, serverMsg: String, localMsg: String)

  class MusitHttpError(val info: MusitHttpErrorInfo) extends Exception(info.localMsg)
  class MusitBadRequest(info: MusitHttpErrorInfo) extends MusitHttpError(info)
  class MusitAuthFailed(info: MusitHttpErrorInfo) extends MusitHttpError(info)

  // TODO: Fix this
  def authFailed(msg: String) = throw new MusitAuthFailed(MusitHttpErrorInfo(None, 401, "", msg))

  implicit class WSRequestImp(val wsr: WSRequest) extends AnyVal {
    def withBearerToken(token: String) = {
      wsr.withHeaders("Authorization" -> ("Bearer " + token))
    }


    // TODO: Handle more exceptions
    def translateStatusToException(resp: WSResponse) =
    {
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
//        println(s"getAndCheckStatus${resp.status}")
        if (resp.status < 200 || resp.status >= 300) {
  //
          println("Future about to fail")
          Future.failed(translateStatusToException(resp))
        }
    else
          Future.successful(resp)
      }

    }

    /*
      def getOrFail() = {
        println("getAndCheck")
        val respF = wsr.get()
        respF.map { resp: WSResponse =>
          println(s"getAndCheckStatus${resp.status}")
          if (resp.status < 200 || resp.status >= 300) {
            val msg = s"Unable to access http resource: ${wsr.uri} Status: ${resp.status} StatusText: ${resp.statusText}"
            println("Future about to fail")
            //Future.failed(throw new Exception(msg))}
            //
            // TODO: Translate to proper async code without exceptions
            //throw new Exception(msg)}
            else
            resp
          }

        }

    */

  }

}
