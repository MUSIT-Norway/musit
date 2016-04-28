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

import play.api.{Application, Logger}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

//import play.api.Play.current
import play.api.cache.Cache

/**
  * Created by jstabel on 4/27/16.
  */
object MusitCache {

  //import play.api.cache.Cache._

  def setFuture[A](key: String, value: Future[A],
                   expiration: Duration = Duration.Inf)
                  (implicit app: Application,
                   ct: ClassTag[A], ec: ExecutionContext): Unit = {
    value.onComplete {
      case Success(v) => Cache.set(key, v, expiration)
        Logger.info(s"CacheSet: Key: $key Value: $v")
      case Failure(v) => Logger.info(s"Cache: Unable to complete future in setFuture: $v") //This hay happen due to http 401, 400 etc. It's not a bug if this fails.
    }
  }

  def getOrElseFuture[A](key: String,
                         expiration: Duration = Duration.Inf)
                        (orElse: => Future[A])
                        (implicit app: Application,
                         ct: ClassTag[A], ec: ExecutionContext): Future[A] = {

    val res = Cache.getAs[A](key)(app, ct)
    res match {
      case Some(v) =>
        Logger.info(s"CacheHit Key: $key Value: $v")
        Future(v)
      case None =>
        val res = orElse
        setFuture(key, res, expiration)
        res
    }
  }

  def getAs[A](key: String)(implicit app: Application, ct: ClassTag[A], ec: ExecutionContext) = play.api.cache.Cache.getAs[A](key)(app, ct)

  def set[A](key: String, value: A,
             expiration: Duration = Duration.Inf)
            (implicit app: Application,
             ct: ClassTag[A], ec: ExecutionContext): Unit = {
    Cache.set(key, value, expiration)
  }
}
