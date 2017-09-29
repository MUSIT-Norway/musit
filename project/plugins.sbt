// Sets the SBT log level
logLevel := Level.Warn

resolvers ++= DefaultOptions.resolvers(snapshot = false)
resolvers += Resolver.typesafeRepo("releases")
resolvers += Resolver.sonatypeRepo("releases")

// Coursier dependency resolver (much improved over default SBT resolution)
addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-RC10")

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.5")

// Formatting and style checking
libraryDependencies += "com.geirsson" %% "scalafmt-bootstrap" % "0.6.6"
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.9.0")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.0")

// Code Coverage plugins
addSbtPlugin("org.scoverage" % "sbt-scoverage"       % "1.5.0")
addSbtPlugin("com.codacy"    % "sbt-codacy-coverage" % "1.3.8")

// Native packaging plugin
addSbtPlugin("com.typesafe.sbt" %% "sbt-native-packager" % "1.2.1")

// I know this because SBT knows this...autogenerates BuildInfo for the project
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")
