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
import com.typesafe.sbt.packager.docker.DockerPlugin
import play.sbt.{Play, PlayLayoutPlugin}
import sbt.Keys._
import sbt._

object CommonSettings {

  val projectSettings = Seq(
    organization := "no.uio.musit",
    scalaVersion := Dependencies.scala,
    resolvers ++= Dependencies.resolvers,
    fork in Test := false,
    parallelExecution in Test := false,
    logBuffered in Test := false,
    scalacOptions := Seq(
      "-deprecation", // Emit warning and location for usages of deprecated APIs.
      "-feature", // Emit warning and location for usages of features that should be imported explicitly.
      "-unchecked", // Enable additional warnings where generated code depends on assumptions.
      // FIXME: the fatal-warnings option must be re-enabled at some point!
    //  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
      "-Xlint", // Enable recommended additional warnings.
      "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
      "-Ywarn-dead-code", // Warn when dead code is identified.
      "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
      "-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
      "-Ywarn-numeric-widen", // Warn when numerics are widened.
      // For advanced language features
      "-language:implicitConversions",
      "-language:higherKinds",
      "-language:existentials",
      "-language:postfixOps",
      "-target:jvm-1.8",
      "-encoding", "UTF-8",
      "-Xmax-classfile-name", "100" // This will limit the classname generation to 240 characters.
    )
  )

  // scalastyle:off
  def BaseProject(name: String): Project =
    Project(name, file(name))
      .settings(projectSettings: _*)
      .settings(Defaults.itSettings: _*)
      .configs(IntegrationTest)

  def PlayProject(name: String): Project =
    BaseProject(name)
      .enablePlugins(
        Play,
        SbtNativePackager,
        DockerPlugin
      )
      .disablePlugins(PlayLayoutPlugin)
      .settings(Defaults.itSettings: _*)
      .configs(IntegrationTest)

}
