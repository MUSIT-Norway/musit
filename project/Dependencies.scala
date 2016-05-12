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
import Keys._

object Dependencies {

  val scala = "2.11.7"

  val resolvers = DefaultOptions.resolvers(snapshot = true)

  object playframework {
    val version      = "2.4.6"
    val slickVersion = "1.1.1"

    val slick        = "com.typesafe.play"   %% "play-slick"               % slickVersion
    val slick_ext    = "com.typesafe.play"   %% "play-slick-evolutions"    % slickVersion
    val jdbc         = "com.typesafe.play"   %% "play-jdbc"                % version
    val cache        = "com.typesafe.play"   %% "play-cache"               % version
    val ws           = "com.typesafe.play"   %% "play-ws"                  % version
    val json         = "com.typesafe.play"   %% "play-json"                % version

  }

  object webjars {
    val webjarsplay  = "org.webjars"         %% "webjars-play"             % "2.4.0-1"
    val bootstrap    = "org.webjars"         %  "bootstrap"                % "3.3.4"
    val requirejs    = "org.webjars"         %  "requirejs"                % "2.1.18"
    val jquery       = "org.webjars"         %  "jquery"                   % "2.1.4"
    val fontawesome  = "org.webjars"         %  "font-awesome"             % "4.3.0-2"
  }

  object documentation {
    val swaggerplay = "io.swagger"          % "swagger-play2_2.11"         % "1.5.1"
    val swaggerUI   = "org.webjars"         %  "swagger-ui"                % "2.1.4"
  }

  val logback        = "ch.qos.logback"      %  "logback-classic"          % "1.1.3"

  val postgresql     = "org.postgresql"      % 	"postgresql" 		           % "9.4-1201-jdbc41"
  val h2database     = "com.h2database"      %  "h2"                       % "1.4.187"
  var scalatestSpec  = "org.scalatest"       %% "scalatest"                % "2.2.4"
  val scalatestplusSpec  = "org.scalatestplus"   %% "play"                     % "1.4.0"
  val scalatest      = scalatestSpec         %  "it,test"
  val scalatestplus  = scalatestplusSpec     %  "it,test"


  // packager for RPM and Docker
  val dockerClient   = "com.spotify" % "docker-client" % "3.2.1"

  val playDependencies: Seq[ModuleID] = Seq(
    playframework.cache,
    playframework.ws,
    playframework.json,
    documentation.swaggerplay,
    documentation.swaggerUI,
    logback //,
    //slf4j
  )

  val testablePlayDependencies: Seq[ModuleID]= playDependencies ++ Seq(
    scalatest,
    scalatestplus
  )


  val playWithPersistenceDependencies: Seq[ModuleID] = playDependencies ++ Seq(
    //postgresql,
    playframework.slick,
    playframework.slick_ext,
    h2database
  )

  val testablePlayWithPersistenceDependencies: Seq[ModuleID]= playWithPersistenceDependencies ++ Seq(
    scalatest,
    scalatestplus
  )
}
