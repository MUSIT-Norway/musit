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
  packageSummary in Docker := "A Microservice for Musit Norway",
  packageDescription in Docker := "A Microservice for Musit Norway",
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
  musitTest,
  musitModels,
  musitService,
  serviceAuth,
  serviceBarcode,
  serviceThingAggregate,
  serviceActor,
  serviceGeoLocation,
  serviceStoragefacility
)

// ======================================================================
// Base projects used as dependencies
// ======================================================================

lazy val musitTest = (
  BaseProject("musit-test")
    settings noPublish
    settings(
      libraryDependencies ++= Seq[ModuleID](
        ScalaTest.scalatestSpec,
        ScalaTest.scalatestplusSpec,
        ScalaTest.scalactic
      ) ++ playDependencies
    )
)

lazy val musitModels = (
  BaseProject("musit-models")
    settings noPublish
    settings(
      libraryDependencies ++= Seq[ModuleID](
        ScalaTest.scalatestSpec,
        ScalaTest.scalatestplusSpec,
        ScalaTest.scalactic,
        PlayFrameWork.json
      )
    )
)

lazy val musitService = (
  BaseProject("musit-service")
    settings noPublish
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(
      libraryDependencies ++= Seq[ModuleID](
        scalaGuice,
        iheartFicus
      )
    )
) dependsOn(musitModels, musitTest % "it,test")

// ======================================================================
// The MUSIT services
// ======================================================================

lazy val serviceAuth = (
  PlayProject("service_auth")
    settings noPublish
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(routesGenerator := InjectedRoutesGenerator)
    settings(scoverageSettings: _*)
    settings(
      baseDockerSettings ++ Seq(
        packageName in Docker := "musit_service_auth"
      )
    )
) dependsOn(musitService, musitTest % Test)

lazy val serviceBarcode = (
  PlayProject("service_barcode")
    settings noPublish
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(libraryDependencies += zxing)
    settings(routesGenerator := InjectedRoutesGenerator)
    settings(scoverageSettings: _*)
    settings(
      baseDockerSettings ++ Seq(
        packageName in Docker := "musit_service_barcode"
      )
    )
) dependsOn(musitService, musitTest % Test)

lazy val serviceThingAggregate = (
  PlayProject("service_thing_aggregate")
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(routesGenerator := InjectedRoutesGenerator)
    settings(scoverageSettings: _*)
    settings(
      baseDockerSettings ++ Seq(
        packageName in Docker := "musit_service_thing_aggregate"
      )
    )
) dependsOn(musitService, musitTest % Test)

lazy val serviceActor = (
  PlayProject("service_actor")
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(routesGenerator := InjectedRoutesGenerator)
    settings(scoverageSettings: _*)
    settings(baseDockerSettings ++ Seq(
      packageName in Docker := "musit_service_actor")
    )
) dependsOn(musitService, musitTest % Test)

lazy val serviceGeoLocation = (
  PlayProject("service_geo_location")
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(routesGenerator := InjectedRoutesGenerator)
    settings(scoverageSettings: _*)
    settings(
      baseDockerSettings ++ Seq(
        packageName in Docker := "musit_service_geo_location"
      )
    )
) dependsOn(musitService, musitTest % Test)

lazy val serviceStoragefacility = (
  PlayProject("service_storagefacility")
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(libraryDependencies ++= enumeratumDeps)
    settings(routesGenerator := InjectedRoutesGenerator)
    settings(scoverageSettings: _*)
    settings(
      baseDockerSettings ++ Seq(
        packageName in Docker := "musit_service_storagefacility"
      )
    )
) dependsOn(musitService, musitTest % Test)
