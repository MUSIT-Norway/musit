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
