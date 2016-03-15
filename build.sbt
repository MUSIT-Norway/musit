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
import CommonSettings._
import Dependencies._
import play.twirl.sbt.Import.TwirlKeys._
import ScoverageSbtPlugin.ScoverageKeys._

name := """musit"""

version := "0.1"

scalacOptions ++= List(
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-target:jvm-1.8",
  "-encoding", "UTF-8"
)

val scoverageSettings = Seq(
  coverageExcludedPackages := "<empty>;controllers.javascript;views.*;router",
  coverageExcludedFiles := "",
  coverageMinimum := 80,
  coverageFailOnMinimum := true
)

lazy val root = (
  project.in(file("."))
  aggregate(common, security_feide, service_example)
)

lazy val common = (
  BaseProject("common")
  settings(libraryDependencies ++= playDependencies)
  settings(scoverageSettings: _*)
)

lazy val security_feide = (
  BaseProject("security_feide")
  settings(libraryDependencies ++= playDependencies)
  settings(scoverageSettings: _*)
)

lazy val service_example = (
  PlayProject("service_example")
  settings(libraryDependencies ++= playWithPersistenceDependencies)
  settings(routesGenerator := InjectedRoutesGenerator)
  settings(scoverageSettings: _*)
) dependsOn(common)

/*
lazy val service2 = (
  PlayProject("service2")
  settings(libraryDependencies ++= playWithPersistenceDependencies)
  settings(routesGenerator := InjectedRoutesGenerator)
  settings(scoverageSettings: _*)
) dependsOn(common) dependsOn(security_feide)
*/

/* Consider adding this as a task for slick automatic code gen from schema
 * https://github.com/slick/slick-codegen-example
 */
