import sbt._

object Dependencies {

  val scala = "2.11.8"

  val resolvers = DefaultOptions.resolvers(snapshot = true) ++ Seq(
    Resolver.bintrayRepo("scalaz", "releases"),
    Resolver.typesafeRepo("releases"),
    Resolver.jcenterRepo
  )

  object PlayFrameWork {
    val version          = play.core.PlayVersion.current // from plugin.sbt
    val playSlickVersion = "2.1.0"

    val slick_play    = "com.typesafe.play" %% "play-slick"            % playSlickVersion
    val slick_play_ev = "com.typesafe.play" %% "play-slick-evolutions" % playSlickVersion
    val jdbc          = "com.typesafe.play" %% "play-jdbc"             % version
    val cache         = "com.typesafe.play" %% "play-cache"            % version
    val ws            = "com.typesafe.play" %% "play-ws"               % version
    val json          = "com.typesafe.play" %% "play-json"             % version
    val logback       = "com.typesafe.play" %% "play-logback"          % version
  }

  object Akka {
    // this need to be synced with play
    val akkaVersion     = "2.4.18"
    val akkaHttpVersion = "10.0.6"
    val akkaOrg         = "com.typesafe.akka"

    val akkaModules =
      Seq(
        "akka-actor",
        "akka-agent",
        "akka-camel",
        "akka-cluster",
        "akka-cluster-metrics",
        "akka-cluster-sharding",
        "akka-cluster-tools",
        "akka-contrib",
        "akka-http-core",
        "akka-http-testkit",
        "akka-multi-node-testkit",
        "akka-osgi",
        "akka-persistence",
        "akka-persistence-tck",
        "akka-remote",
        "akka-slf4j",
        "akka-stream",
        "akka-stream-testkit",
        "akka-testkit"
      )

    val akkaHttpModuels = Seq(
      "akka-http-core",
      "akka-http",
      "akka-http-testkit",
      "akka-http-jackson",
      "akka-http-xml"
    )

    val akkaDependencyOverrides = akkaModules.map(akkaOrg %% _ % akkaVersion) ++
      akkaHttpModuels.map(akkaOrg %% _ % akkaVersion)

    val akkaTestKit = akkaOrg %% "akka-testkit" % akkaVersion % Test
  }

  object Netty {
    val reactiveStreamsHttp = "com.typesafe.netty" % "netty-reactive-streams-http" % "1.0.8"
  }

  object Logging {
    val logbackVersion = "1.2.3"
    val slf4jVersion   = "1.7.25"
    val logback        = "ch.qos.logback" % "logback-classic" % logbackVersion
    val log4jBridge    = "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.8.2"
    val slf4jLibs      = Seq("slf4j-api", "jul-to-slf4j", "jcl-over-slf4j")
    val slf4j          = slf4jLibs.map("org.slf4j" % _ % slf4jVersion)
    val slf4jApi       = "org.slf4j" % slf4jLibs.head % slf4jVersion
    val loggingDeps    = slf4j ++ Seq(logback, log4jBridge)
  }

  object ScalaTest {
    val scalaTestVersion     = "3.0.1"
    val scalaTestPlusVersion = "2.0.0"

    var scalatestSpec = "org.scalatest" %% "scalatest" % scalaTestVersion
    val scalactic     = "org.scalactic" %% "scalactic" % scalaTestVersion

    val scalatestplusSpec = "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion

    val scalatest     = scalatestSpec     % Test
    val scalatestplus = scalatestplusSpec % Test
  }

  val scalaMock = "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0" % Test

  val iheartFicus = "com.iheart"       %% "ficus"       % "1.4.0"
  val scalaGuice  = "net.codingwell"   %% "scala-guice" % "4.1.0"
  val h2database  = "com.h2database"   % "h2"           % "1.4.194"
  val zxing       = "com.google.zxing" % "core"         % "3.3.0"
  val zxingClient = "com.google.zxing" % "javase"       % "3.3.0" % Test

  // Oracle specifics
  def dir    = new java.io.File(".").getCanonicalPath
  val oracle = "com.oracle" % "ojdbc7" % "my" from s"file://$dir/libs/ojdbc7.jar"

  val enumeratumDeps: Seq[ModuleID] = {
    val enumeratumVersion = "1.5.10"
    val libs              = Seq("enumeratum", "enumeratum-play", "enumeratum-play-json")
    libs.map("com.beachape" %% _ % enumeratumVersion)
  }

  val elastic4sVersion: String = "5.4.9"
  val elastic4s: Seq[ModuleID] = Seq(
    "com.sksamuel.elastic4s" %% "elastic4s-core"         % elastic4sVersion,
    "com.sksamuel.elastic4s" %% "elastic4s-http-streams" % elastic4sVersion,
    "com.sksamuel.elastic4s" %% "elastic4s-play-json"    % elastic4sVersion
      excludeAll ExclusionRule(organization = "com.typesafe.play")
  )
  val playDependencies: Seq[ModuleID] = Seq(
    PlayFrameWork.cache,
    PlayFrameWork.ws,
    PlayFrameWork.json,
    iheartFicus
  ) ++ Logging.loggingDeps

  val testablePlayDependencies: Seq[ModuleID] = playDependencies ++ Seq(
    ScalaTest.scalatest,
    ScalaTest.scalatestplus,
    ScalaTest.scalactic,
    scalaMock,
    Akka.akkaTestKit
  )

  val playWithPersistenceDependencies: Seq[ModuleID] = playDependencies ++ Seq(
    PlayFrameWork.slick_play,
    PlayFrameWork.slick_play_ev,
    h2database,
    oracle
  )

  val testablePlayWithPersistenceDependencies: Seq[ModuleID] =
    playWithPersistenceDependencies ++ Seq(
      ScalaTest.scalatest,
      ScalaTest.scalatestplus,
      ScalaTest.scalactic,
      scalaMock,
      Akka.akkaTestKit
    )
}
