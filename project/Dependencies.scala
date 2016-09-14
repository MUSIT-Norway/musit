/*
 *  MUSIT is a museum database to archive natural and cultural history data.
 *  Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
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

import sbt._

object Dependencies {

  val scala = "2.11.8"

  val resolvers = DefaultOptions.resolvers(snapshot = true) ++ Seq(
    Resolver.bintrayRepo("scalaz", "releases")
  )

  object PlayFrameWork {
    val version = "2.5.7"
    val slickVersion = "2.0.0"

    val slick = "com.typesafe.play" %% "play-slick" % slickVersion
    val slick_ext = "com.typesafe.play" %% "play-slick-evolutions" % slickVersion
    val jdbc = "com.typesafe.play" %% "play-jdbc" % version
    val cache = "com.typesafe.play" %% "play-cache" % version
    val ws = "com.typesafe.play" %% "play-ws" % version
    val json = "com.typesafe.play" %% "play-json" % version
    val logback = "com.typesafe.play" %% "play-logback" % version

  }

  object PlayJson {
    val derivedCodecs = "org.julienrf" %% "play-json-derived-codecs" % "3.3"
  }

  object Enumeratum {
    val enumeratum = "com.beachape" %% "enumeratum" % "1.4.4"
    val enumeratumPlayJson = "com.beachape" %% "enumeratum-play-json" % "1.4.4"
    val enumeratumPlay = "com.beachape" %% "enumeratum-play" % "1.4.4"
  }

  val logback = "ch.qos.logback" % "logback-classic" % "1.1.7"

  val postgresql = "org.postgresql" % "postgresql" % "9.4-1201-jdbc41"
  val h2database = "com.h2database" % "h2" % "1.4.192"
  var scalatestSpec = "org.scalatest" %% "scalatest" % "2.2.4"
  val scalatestplusSpec = "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1"
  val scalatest = scalatestSpec % "it,test"
  val scalatestplus = scalatestplusSpec % "it,test"


  val enumeratumDependencies: Seq[ModuleID] = {
    val enumeratumVersion = "1.4.10"
    Seq(
      "com.beachape" %% "enumeratum" % enumeratumVersion,
      "com.beachape" %% "enumeratum-play-json" % enumeratumVersion,
      "com.beachape" %% "enumeratum-play" % enumeratumVersion
    )
  }

  val playJsDerivedCodecs = "org.julienrf" %% "play-json-derived-codecs" % "3.3"

  // packager for RPM and Docker
  val dockerClient = "com.spotify" % "docker-client" % "3.2.1"

  val playDependencies: Seq[ModuleID] = Seq(
    PlayFrameWork.cache,
    PlayFrameWork.ws,
    PlayFrameWork.json,
    logback
  )

  val testablePlayDependencies: Seq[ModuleID] = playDependencies ++ Seq(
    scalatest,
    scalatestplus
  )


  val playWithPersistenceDependencies: Seq[ModuleID] = playDependencies ++ Seq(
    PlayFrameWork.slick,
    PlayFrameWork.slick_ext,
    postgresql,
    h2database
  )

  val testablePlayWithPersistenceDependencies: Seq[ModuleID] = playWithPersistenceDependencies ++ Seq(
    scalatest,
    scalatestplus
  )
}
