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

// Sets the SBT log level
logLevel := Level.Warn

resolvers ++= DefaultOptions.resolvers(snapshot = false)
resolvers += Resolver.typesafeRepo("releases")
resolvers += Resolver.sonatypeRepo("releases")

// Coursier dependency resolver (much improved over default SBT resolution)
addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-M15-1")

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.13")

// Formatting and style checking
addSbtPlugin("com.geirsson"   % "sbt-scalafmt"           % "0.6.3")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.0")

// Code Coverage plugins
addSbtPlugin("org.scoverage" % "sbt-scoverage"       % "1.5.0")
addSbtPlugin("com.codacy"    % "sbt-codacy-coverage" % "1.3.4")

// Native packaging plugin
addSbtPlugin("com.typesafe.sbt" %% "sbt-native-packager" % "1.1.4")

// I know this because SBT knows this...autogenerates BuildInfo for the project
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.6.1")
