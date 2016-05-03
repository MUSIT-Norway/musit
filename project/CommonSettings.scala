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

import com.typesafe.sbt.SbtNativePackager
import com.typesafe.sbt.SbtNativePackager.autoImport._
import com.typesafe.sbt.packager.docker.DockerPlugin
import sbt._
import Keys._

object CommonSettings {

  val projectSettings = Seq(
    organization := "no.uio.musit",
    scalaVersion := Dependencies.scala,
    resolvers ++= Dependencies.resolvers,
    fork in Test := false,
    parallelExecution in Test := true
  )

  def BaseProject(name: String): Project = (
    Project(name, file(name))
    settings(projectSettings:_*)
    settings(Defaults.itSettings: _*)
    configs(IntegrationTest)
  )

  def PlayProject(name: String): Project = (
    BaseProject(name)
    enablePlugins(play.sbt.Play)
    enablePlugins(SbtNativePackager)
    enablePlugins(DockerPlugin)
    settings(Defaults.itSettings: _*)
    configs(IntegrationTest)
    //enablePlugins(DockerSpotifyClientPlugin) get spotify client running and you have no dependencies to local docker
  )
  
}
