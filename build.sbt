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
import scoverage.ScoverageKeys._

name := """musit"""

version := "0.1"

import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import sbt._
import scalariform.formatter.preferences._


scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(FormatXml, false)
  .setPreference(SpacesAroundMultiImports, false)

val baseDockerSettings = Seq(
  maintainer in Docker := "Musit Norway <musit@musit.uio.no>",
  packageSummary in Docker := "A Microservice part of the middleware for Musit Norway",
  packageDescription in Docker := "A Microservice part of the middleware for MusitNorway",
  dockerExposedPorts in Docker := Seq(8080)
)

val scoverageSettings = Seq(
  coverageExcludedPackages := "<empty>;controllers.javascript;views.*;router",
  coverageExcludedFiles := "",
  coverageMinimum := 80,
  coverageFailOnMinimum := false
)

val noPublish = Seq(
  publish := {},
  publishLocal := {}
)

lazy val root = project in file(".") settings noPublish aggregate(
  common_test,
  common,
  security,
  service_core,
  service_musit_thing,
  service_actor,
  service_geo_location,
  service_time,
  service_storage_admin,
  service_event,
  service_storagefacility
)

// Base projects used as dependencies
lazy val common = (
  BaseProject("common")
    settings noPublish
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(libraryDependencies += PlayFrameWork.logback)
    settings(scoverageSettings: _*)
  ) dependsOn(common_test % "it,test")

lazy val common_test = (
  BaseProject("common_test")
    settings noPublish
    settings(libraryDependencies ++= playWithPersistenceDependencies ++ Seq[ModuleID](scalatestSpec))
    settings(scoverageSettings: _*)
  )

lazy val security = (
  BaseProject("security")
    settings noPublish
    settings(libraryDependencies ++= testablePlayDependencies)
    settings(scoverageSettings: _*)
  )  dependsOn(common, common_test % "it,test")

lazy val service_core = (
  PlayProject("service_core")
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(routesGenerator := InjectedRoutesGenerator)
    settings(scoverageSettings: _*)
    settings(baseDockerSettings ++ Seq(
    packageName in Docker := "musit_service_core"
  ))
  ) dependsOn(common, security, common_test % "it,test")

// Add other services here


lazy val service_musit_thing = (
  PlayProject("service_musit_thing")
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(routesGenerator := InjectedRoutesGenerator)
    settings(scoverageSettings: _*)
    settings(baseDockerSettings ++ Seq(
    packageName in Docker := "musit_service_musit_thing"
  ))
  )  dependsOn(common, common_test % "it,test")

lazy val service_actor = (
  PlayProject("service_actor")
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(routesGenerator := InjectedRoutesGenerator)
    settings(scoverageSettings: _*)
    settings(baseDockerSettings ++ Seq(packageName in Docker := "musit_service_actor"))
  )  dependsOn(common, security, common_test % "it,test")

lazy val service_geo_location = (
  PlayProject("service_geo_location")
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(routesGenerator := InjectedRoutesGenerator)
    settings(scoverageSettings: _*)
    settings(baseDockerSettings ++ Seq(
    packageName in Docker := "musit_service_geo_location"
  ))
  )  dependsOn(common, common_test % "it,test")


lazy val service_time = (
  PlayProject("service_time")
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(routesGenerator := InjectedRoutesGenerator)
    settings(scoverageSettings: _*)
    settings(baseDockerSettings ++ Seq(
    packageName in Docker := "musit_service_time"
  ))
  )  dependsOn(common, common_test % "it,test")

lazy val service_storage_admin = (
  PlayProject("service_storage_admin")
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(libraryDependencies ++= enumeratumDependencies)
    settings(libraryDependencies += playJsDerivedCodecs)
    settings(routesGenerator := InjectedRoutesGenerator)
    settings(scoverageSettings: _*)
    settings(baseDockerSettings ++ Seq(
      packageName in Docker := "musit_service_storage_admin"
    ))
  )  dependsOn(common, common_test % "it,test")

lazy val service_storagefacility = (
  PlayProject("service_storagefacility")
  settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
  settings(libraryDependencies ++= enumeratumDependencies)
  settings(libraryDependencies += playJsDerivedCodecs)
  settings(routesGenerator := InjectedRoutesGenerator)
  settings(scoverageSettings: _*)
  settings(baseDockerSettings ++ Seq(
    packageName in Docker := "musit_service_storagefacility"
  ))
)


lazy val service_event = (
  PlayProject("service_event")
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(routesGenerator := InjectedRoutesGenerator)
    settings(scoverageSettings: _*)
    settings(baseDockerSettings ++ Seq(
    packageName in Docker := "musit_service_event"
  ))
  )  dependsOn(common, security, common_test % "it,test")
