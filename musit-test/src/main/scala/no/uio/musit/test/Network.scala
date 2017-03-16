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

package no.uio.musit.test

import java.io.IOException
import java.net.{ServerSocket => JServerSocket}

import play.api.test.Helpers

import scala.util.{Random, Try}

trait Network {

  def generatePort: Int = {
    Try {
      val portnum = Helpers.testServerPort + Random.nextInt(500)
      val socket  = new JServerSocket(portnum)
      socket.close()
      portnum
    }.recover {
      // In case we try opening the same port twice.
      case ioe: IOException => generatePort

    }.getOrElse(Helpers.testServerPort)
  }

}
