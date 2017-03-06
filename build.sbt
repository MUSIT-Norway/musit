import CommonSettings._
import Dependencies._
import scoverage.ScoverageKeys._

name := """musit"""

version := "0.1"

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
  serviceStoragefacility,
  serviceAnalysis
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
    settings(libraryDependencies += Netty.reactiveStreamsHttp)
    settings(routesGenerator := InjectedRoutesGenerator)
    settings(packageName in Docker := "musit_service_auth")
) dependsOn(musitService, musitTest % Test)

lazy val serviceBarcode = (
  PlayProject("service_barcode")
    settings noPublish
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(libraryDependencies += zxing)
    settings(routesGenerator := InjectedRoutesGenerator)
    settings(packageName in Docker := "musit_service_barcode")
) dependsOn(musitService, musitTest % Test)

lazy val serviceThingAggregate = (
  PlayProject("service_thing_aggregate")
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(routesGenerator := InjectedRoutesGenerator)
    settings(packageName in Docker := "musit_service_thing_aggregate")
) dependsOn(musitService, musitTest % Test)

lazy val serviceActor = (
  PlayProject("service_actor")
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(routesGenerator := InjectedRoutesGenerator)
    settings(packageName in Docker := "musit_service_actor")
) dependsOn(musitService, musitTest % Test)

lazy val serviceGeoLocation = (
  PlayProject("service_geo_location")
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(routesGenerator := InjectedRoutesGenerator)
    settings(packageName in Docker := "musit_service_geo_location")
) dependsOn(musitService, musitTest % Test)

lazy val serviceStoragefacility = (
  PlayProject("service_storagefacility")
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(libraryDependencies ++= enumeratumDeps)
    settings(routesGenerator := InjectedRoutesGenerator)
    settings(packageName in Docker := "musit_service_storagefacility")
) dependsOn(musitService, musitTest % Test)

lazy val serviceAnalysis = (
  PlayProject("service_analysis")
    settings(libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings(libraryDependencies ++= enumeratumDeps)
    settings(routesGenerator := InjectedRoutesGenerator)
    settings(packageName in Docker := "musit_service_analysis")
  ) dependsOn(musitService, musitTest % Test)