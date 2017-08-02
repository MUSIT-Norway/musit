import CommonSettings._
import Dependencies._

name := """musit"""

version := "0.1"

val noPublish = Seq(
  publish := {},
  publishLocal := {}
)

lazy val root = project in file(".") settings noPublish aggregate (
  musitTest,
  musitModels,
  musitService,
  serviceAuth,
  serviceBarcode,
  serviceBackend
)

// ======================================================================
// Base projects used as dependencies
// ======================================================================

lazy val musitTest = (
  BaseProject("musit-test")
    settings noPublish
    settings (
      libraryDependencies ++= Seq[ModuleID](
        ScalaTest.scalatestSpec,
        ScalaTest.scalatestplusSpec,
        ScalaTest.scalactic
      ) ++ playDependencies
    )
) dependsOn (musitModels)

lazy val musitModels = (
  BaseProject("musit-models")
    settings noPublish
    settings (
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
    settings (libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings (
      libraryDependencies ++= Seq[ModuleID](
        scalaGuice,
        iheartFicus
      )
    )
) dependsOn (musitModels, musitTest % "it,test")

// ======================================================================
// The MUSIT services
// ======================================================================

lazy val serviceAuth = (
  PlayProject("service_auth")
    settings noPublish
    settings (libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings (libraryDependencies += Netty.reactiveStreamsHttp)
    settings (routesGenerator := InjectedRoutesGenerator)
    settings (packageName in Docker := "musit_service_auth")
) dependsOn (musitService, musitTest % Test)

lazy val serviceBarcode = (
  PlayProject("service_barcode")
    settings noPublish
    settings (libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings (libraryDependencies ++= Seq(zxing, zxingClient))
    settings (routesGenerator := InjectedRoutesGenerator)
    settings (packageName in Docker := "musit_service_barcode")
) dependsOn (musitService, musitTest % Test)

lazy val serviceBackend = (
  PlayProject("service_backend")
    settings (libraryDependencies ++= testablePlayWithPersistenceDependencies)
    settings (libraryDependencies ++= enumeratumDeps ++ elastic4s)
    settings (routesGenerator := InjectedRoutesGenerator)
    settings (packageName in Docker := "musit_service_backend")
) dependsOn (musitService, musitTest % Test)
