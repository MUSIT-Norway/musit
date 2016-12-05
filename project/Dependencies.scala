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

object Dependencies {

  val scala = "2.11.8"

  val resolvers = DefaultOptions.resolvers(snapshot = true) ++ Seq(
    Resolver.bintrayRepo("scalaz", "releases"),
    Resolver.typesafeRepo("releases"),
    Resolver.jcenterRepo
  )

  object PlayFrameWork {
    val version = "2.5.10"
    val playSlickVersion = "2.0.0"

    val slick_play = "com.typesafe.play" %% "play-slick" % playSlickVersion
    val slick_play_ev = "com.typesafe.play" %% "play-slick-evolutions" % playSlickVersion
    val slick_ext = "com.typesafe.slick" %% "slick-extensions" % "3.1.0"
    val jdbc = "com.typesafe.play" %% "play-jdbc" % version
    val cache = "com.typesafe.play" %% "play-cache" % version
    val ws = "com.typesafe.play" %% "play-ws" % version
    val json = "com.typesafe.play" %% "play-json" % version
    val logback = "com.typesafe.play" %% "play-logback" % version
  }

  object Silhouette {
    val silhouetteVersion = "4.0.0"
    val silhouette = "com.mohiva" %% "play-silhouette" % silhouetteVersion
    val silhouetteBcrypt = "com.mohiva" %% "play-silhouette-password-bcrypt" % silhouetteVersion
    val silhouetteCrypto = "com.mohiva" %% "play-silhouette-crypto-jca" % silhouetteVersion
    val silhouettePersistence = "com.mohiva" %% "play-silhouette-persistence" % silhouetteVersion
    val silhouetteTestkit = "com.mohiva" %% "play-silhouette-testkit" % silhouetteVersion % Test

    val allDeps = Seq(
      silhouette,
      silhouetteBcrypt,
      silhouetteCrypto,
      silhouettePersistence,
      silhouetteTestkit
    )
  }

  object Logging {
    val logbackVersion = "1.1.7"
    val slf4jVersion = "1.7.21"
    val logback = "ch.qos.logback" % "logback-classic" % logbackVersion
    val slf4jLibs = Seq("slf4j-api", "jul-to-slf4j", "jcl-over-slf4j")
    val slf4j = slf4jLibs.map("org.slf4j" % _ % slf4jVersion)
    val slf4jApi = "org.slf4j" % slf4jLibs.head % slf4jVersion
    val loggingDeps = slf4j ++ Seq(logback)
  }

  object ScalaTest {
    val scalaTestVersion = "2.2.6" // "3.0.0"
    val scalaTestPlusVersion = "1.5.1" // "2.0.0-M1"

    var scalatestSpec = "org.scalatest" %% "scalatest" % scalaTestVersion
    val scalactic = "org.scalactic" %% "scalactic" % scalaTestVersion

    val scalatestplusSpec = "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion

    val scalatest = scalatestSpec % Test
    val scalatestplus = scalatestplusSpec % Test
  }

  val iheartFicus = "com.iheart" %% "ficus" % "1.2.3"
  val scalaGuice = "net.codingwell" %% "scala-guice" % "4.1.0"
  val postgresql = "org.postgresql" % "postgresql" % "9.4-1201-jdbc41"
  val h2database = "com.h2database" % "h2" % "1.4.192"
  val zxing = "com.google.zxing" % "core" % "3.3.0"
  def dir = new java.io.File(".").getCanonicalPath
  val oracle = "com.oracle" % "ojdbc7" % "my" from s"file://$dir/libs/ojdbc7.jar"

  val enumeratumDeps: Seq[ModuleID] = {
    val enumeratumVersion = "1.4.10"
    val libs = Seq("enumeratum", "enumeratum-play", "enumeratum-play-json")
    libs.map("com.beachape" %% _ % enumeratumVersion)
  }

  val playDependencies: Seq[ModuleID] = Seq(
    PlayFrameWork.cache,
    PlayFrameWork.ws,
    PlayFrameWork.json
  ) ++ Logging.loggingDeps

  val testablePlayDependencies: Seq[ModuleID] = playDependencies ++ Seq(
    ScalaTest.scalatest,
    ScalaTest.scalatestplus,
    ScalaTest.scalactic
  )


  val playWithPersistenceDependencies: Seq[ModuleID] = playDependencies ++ Seq(
    PlayFrameWork.slick_play,
    PlayFrameWork.slick_play_ev,
    PlayFrameWork.slick_ext,
    postgresql,
    h2database,
    oracle
  )

  val testablePlayWithPersistenceDependencies: Seq[ModuleID] =
    playWithPersistenceDependencies ++ Seq(
      ScalaTest.scalatest,
      ScalaTest.scalatestplus,
      ScalaTest.scalactic
    )
}
