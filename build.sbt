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
  commonTest,
  musitTest,
  musitService,
  common,
  security,
  serviceCore,
  serviceThingAggregate,
  serviceActor,
  serviceGeoLocation,
  serviceStoragefacility
)

// Base projects used as dependencies

// TODO: Move the good parts into musit-service and other modules.
lazy val common = (
  BaseProject("common")
    settings noPublish
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(libraryDependencies += PlayFrameWork.logback)
    settings(scoverageSettings: _*)
  ) dependsOn(commonTest % "it,test")

@deprecated(message = "Use musit-test instead.")
lazy val commonTest = (
  BaseProject("common_test")
    settings noPublish
    settings(libraryDependencies ++= playWithPersistenceDependencies ++ Seq[ModuleID](scalatestSpec))
    settings(scoverageSettings: _*)
  )

lazy val musitTest = (
  BaseProject("musit-test")
    settings noPublish
    settings(
      libraryDependencies ++= Seq[ModuleID](
        scalatestSpec,
        scalatestplusSpec
      ) ++ playDependencies
    )
  )

lazy val musitService = (
  BaseProject("musit-service")
    settings noPublish
    settings(
      libraryDependencies ++= Seq[ModuleID](
        scalatest,
        PlayFrameWork.json
      )
    )
)

lazy val security = (
  BaseProject("security")
    settings noPublish
    settings(libraryDependencies ++= testablePlayDependencies)
    settings(scoverageSettings: _*)
  )  dependsOn(common, commonTest % "it,test")

lazy val serviceCore = (
  PlayProject("service_core")
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(routesGenerator := InjectedRoutesGenerator)
    settings(scoverageSettings: _*)
    settings(baseDockerSettings ++ Seq(
    packageName in Docker := "musit_service_core"
  ))
  ) dependsOn(common, security, commonTest % "it,test")

lazy val serviceThingAggregate = (
  PlayProject("service_thing_aggregate")
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(routesGenerator := InjectedRoutesGenerator)
    settings(scoverageSettings: _*)
    settings(baseDockerSettings ++ Seq(
      packageName in Docker := "musit_service_thing_aggregate"
    ))
  ) dependsOn(musitService, musitTest % "it,test")

lazy val serviceActor = (
  PlayProject("service_actor")
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(routesGenerator := InjectedRoutesGenerator)
    settings(scoverageSettings: _*)
    settings(baseDockerSettings ++ Seq(packageName in Docker := "musit_service_actor"))
  )  dependsOn(common, security, commonTest % "it,test")

lazy val serviceGeoLocation = (
  PlayProject("service_geo_location")
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(routesGenerator := InjectedRoutesGenerator)
    settings(scoverageSettings: _*)
    settings(baseDockerSettings ++ Seq(
    packageName in Docker := "musit_service_geo_location"
  ))
  )  dependsOn(common, commonTest % "it,test")

lazy val serviceStoragefacility = (
  PlayProject("service_storagefacility")
  settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
  settings(libraryDependencies ++= enumeratumDependencies)
  settings(routesGenerator := InjectedRoutesGenerator)
  settings(scoverageSettings: _*)
  settings(baseDockerSettings ++ Seq(
    packageName in Docker := "musit_service_storagefacility"
  ))
) dependsOn(musitService, musitTest % "it,test")