import Dependencies.ScalaTest
import com.typesafe.sbt.SbtNativePackager
import com.typesafe.sbt.SbtNativePackager.autoImport._
import com.typesafe.sbt.packager.docker.DockerPlugin
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import play.sbt.PlayImport.PlayKeys
import play.sbt.Play
import sbt.Keys._
import sbt._
import sbtbuildinfo.BuildInfoPlugin
import sbtbuildinfo.BuildInfoPlugin.autoImport._
import scoverage.ScoverageKeys._

object CommonSettings {

  val projectSettings = Seq(
    organization := "no.uio.musit",
    scalaVersion := Dependencies.scala,
    resolvers ++= Dependencies.resolvers,
    // Setting forking for tests explicitly to true to avoid OOME in local dev env.
    fork in Test := true,
    // Run tests sequentially
    parallelExecution in Test := false,
    // Print log statements as they happen instead of doing it out of band.
    logBuffered in Test := false,
    // Compiler flags that enable certain language features, and adds
    // checks to prevent "bad" code.
    scalacOptions := Seq(
      "-deprecation", // Emit warning and location for usages of deprecated APIs.
      "-feature", // Emit warning and location for usages of features that should be imported explicitly.
      "-unchecked", // Enable additional warnings where generated code depends on assumptions.
      "-Xfatal-warnings", // Fail the compilation if there are any warnings.
      "-Xlint", // Enable recommended additional warnings.
      "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
      "-Ywarn-dead-code", // Warn when dead code is identified.
      "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
      "-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
      "-Ywarn-numeric-widen", // Warn when numerics are widened.
      "-Xfuture",
      // For advanced language features
      "-language:implicitConversions",
      "-language:higherKinds",
      "-language:existentials",
      "-language:postfixOps",
      "-target:jvm-1.8",
      "-encoding",
      "UTF-8",
      "-Xmax-classfile-name",
      "100" // This will limit the classname generation to 100 characters.
    ),
    // Configuring the scoverage plugin.
    coverageExcludedPackages := "<empty>;router;controllers.javascript;" +
      "controllers.web;views.*;no.uio.musit.test;migration",
    coverageExcludedFiles := "",
    coverageMinimum := 80,
    coverageFailOnMinimum := false,
    // Disable scaladoc
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in packageDoc := false,
    sources in (Compile, doc) := Seq.empty
  )

  lazy val AllTests       = config("allTests") extend Test
  lazy val ContainerTests = config("containerTests") extend Test

  def testArg(key: String, value: String) =
    Tests.Argument(TestFrameworks.ScalaTest, key, value)

  // scalastyle:off
  def BaseProject(projName: String): Project =
    Project(projName, file(projName))
      .settings(projectSettings: _*)
      .settings(
        // Setting timezone for testing to UTC, because h2 doesn't support
        // timezones very well, and it will always default to UTC regardless.
        // For production environments we're using the timezone configured at
        // OS level for each running service.
        javaOptions in Test += "-Duser.timezone=UTC"
      )
      .settings(dependencyOverrides += ScalaTest.scalatest)
      .settings(Dependencies.Akka.akkaDependencyOverrides.map(dependencyOverrides += _))
      .configs(IntegrationTest)

  // Check if the build is being run on internal GitLab CI runner.
  // If so, we need to use the internal docker registry to pull images from.
  val dockerRegistryHost      = scala.util.Properties.envOrNone("MUSIT_DOCKER_REGISTRY")
  val dockerMusitBaseImage    = scala.util.Properties.envOrNone("MUSIT_BASE_IMAGE")
  val dockerRegistryNamespace = "musit"

  val commitSha = scala.util.Properties.envOrElse("CI_COMMIT_SHA", "not built on CI")

  def PlayProject(projName: String): Project =
    BaseProject(projName)
      .enablePlugins(
        Play,
        BuildInfoPlugin,
        SbtNativePackager,
        DockerPlugin
      )
      .configs(AllTests, ContainerTests)
      .settings(
        inConfig(ContainerTests)(Defaults.testTasks),
        inConfig(AllTests)(Defaults.testTasks),
        testOptions in Test := Seq(testArg("-l", "musit.ElasticsearchContainer")), // exclude
        testOptions in ContainerTests := Seq(
          testArg("-n", "musit.ElasticsearchContainer")
        ), // include
        testOptions in AllTests := Seq()
      )
      .settings(
        dependencyOverrides += "com.typesafe.play" %% "play-logback" % Dependencies.PlayFrameWork.version
      )
      .settings(
        Seq(
          PlayKeys.playOmnidoc := false,
          buildInfoKeys := Seq[BuildInfoKey](
            name,
            version,
            scalaVersion,
            sbtVersion,
            buildInfoBuildNumber,
            "commitSha" -> commitSha
          ),
          buildInfoPackage := "no.uio.musit.service",
          buildInfoOptions ++= Seq(BuildInfoOption.ToJson, BuildInfoOption.BuildTime),
          javaOptions in Test ++= Seq(
            "-Dconfig.file=conf/application.test.conf",
            "-Dlogger.resource=logback-test.xml"
          ),
          // Docker packaging configuration
          maintainer in Docker := "Musit Norway <musit@musit.uio.no>",
          packageSummary in Docker := "A Microservice part of the middleware for Musit Norway",
          packageDescription in Docker := "A Microservice part of the middleware for MusitNorway",
          dockerExposedPorts := Seq(8080),
          dockerExposedVolumes := Seq("/opt/docker/logs"),
          dockerBaseImage := s"${dockerRegistryHost.map(_ => "library/java:8").getOrElse("openjdk:8")}",
          dockerRepository := dockerRegistryHost
            .map(host => s"$host/$dockerRegistryNamespace"),
          dockerUpdateLatest := true,
          dockerAlias := DockerAlias(
            registryHost = dockerRepository.value,
            username = None,
            name = packageName.value,
            tag = dockerRegistryHost.map(_ => "utv").orElse(Some(version.value))
          )
        )
      )

}
