import sbt._

object Dependencies {

  val scala = "2.11.8"

  val ScalaFmtVersion = "1.2.0"

  val resolvers = DefaultOptions.resolvers(snapshot = true) ++ Seq(
    Resolver.bintrayRepo("scalaz", "releases"),
    Resolver.typesafeRepo("releases"),
    Resolver.jcenterRepo,
    Resolver.defaultLocal
  )

  object PlayFrameWork {
    val version          = play.core.PlayVersion.current // from plugin.sbt
    val playSlickVersion = "3.0.1"

    val slick_play    = "com.typesafe.play" %% "play-slick"            % playSlickVersion
    val slick_play_ev = "com.typesafe.play" %% "play-slick-evolutions" % playSlickVersion
    val jdbc          = "com.typesafe.play" %% "play-jdbc"             % version
    val cache         = "com.typesafe.play" %% "play-cache"            % version
    val ws            = "com.typesafe.play" %% "play-ws"               % version
    val guice         = "com.typesafe.play" %% "play-guice"            % version
    val json          = "com.typesafe.play" %% "play-json"             % version
    val jsonJoda      = "com.typesafe.play" %% "play-json-joda"        % version
    val logback       = "com.typesafe.play" %% "play-logback"          % version
  }

  object Akka {
    // this need to be synced with play
    val akkaVersion     = "2.5.4"
    val akkaHttpVersion = "10.0.9"
    val akkaOrg         = "com.typesafe.akka"

    val akkaModules =
      Seq(
        "akka-actor",
        "akka-agent",
        "akka-cluster",
        "akka-cluster-metrics",
        "akka-cluster-sharding",
        "akka-cluster-tools",
        "akka-contrib",
        "akka-multi-node-testkit",
        "akka-persistence",
        "akka-persistence-tck",
        "akka-remote",
        "akka-slf4j",
        "akka-stream",
        "akka-stream-testkit",
        "akka-testkit"
      )

    val akkaHttpModules = Seq(
      "akka-http-core",
      "akka-http",
      "akka-http-testkit",
      "akka-http-jackson",
      "akka-http-xml"
    )

    val akkaDependencyOverrides = akkaModules.map(akkaOrg %% _ % akkaVersion) ++
      akkaHttpModules.map(akkaOrg %% _ % akkaHttpVersion)

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
    val scalaTestVersion     = "3.0.3"
    val scalaTestPlusVersion = "3.1.1"

    var scalatestSpec = "org.scalatest" %% "scalatest" % scalaTestVersion
    val scalactic     = "org.scalactic" %% "scalactic" % scalaTestVersion

    val scalatestplusSpec = "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion

    val scalatest     = scalatestSpec     % Test
    val scalatestplus = scalatestplusSpec % Test
  }

  val scalaMock = "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0" % Test

  val JodaTime    = "joda-time"        % "joda-time"    % "2.9.9"
  val JodaConvert = "org.joda"         % "joda-convert" % "1.8.1"
  val iheartFicus = "com.iheart"       %% "ficus"       % "1.4.1"
  val scalaGuice  = "net.codingwell"   %% "scala-guice" % "4.1.0"
  val h2database  = "com.h2database"   % "h2"           % "1.4.194"
  val zxing       = "com.google.zxing" % "core"         % "3.3.0"
  val zxingClient = "com.google.zxing" % "javase"       % "3.3.0" % Test
  val scalatags   = "com.lihaoyi"      %% "scalatags"   % "0.6.7"

  // Oracle specifics
  def dir = new java.io.File(".").getCanonicalPath

  val oracle = "com.oracle" % "ojdbc7" % "my" from s"file://$dir/libs/ojdbc7.jar"

  val enumeratumDeps: Seq[ModuleID] = {
    Seq(
      "com.beachape" %% "enumeratum"           % "1.5.12",
      "com.beachape" %% "enumeratum-play"      % "1.5.12-2.6.0-M5",
      "com.beachape" %% "enumeratum-play-json" % "1.5.12-2.6.0-M7"
    )
  }

  val elastic4sVersion: String = "5.5.10"
  val elastic4s: Seq[ModuleID] = Seq(
    "com.sksamuel.elastic4s" %% "elastic4s-core"         % elastic4sVersion,
    "com.sksamuel.elastic4s" %% "elastic4s-http-streams" % elastic4sVersion,
    "com.sksamuel.elastic4s" %% "elastic4s-play-json"    % elastic4sVersion
      excludeAll ExclusionRule(organization = "com.typesafe.play")
  )

  // Symbiotic dependencies
  object Symbiotic {
    val symbioticVersion = "0.1.14"
    val artifacts = Seq(
      "symbiotic-play",
      "symbiotic-json",
      "symbiotic-core",
      "symbiotic-postgres",
      "symbiotic-elasticsearch"
    )

    val libs: Seq[ModuleID] = artifacts.map("net.scalytica" %% _ % symbioticVersion)

    val testkit = "net.scalytica" %% "symbiotic-testkit" % symbioticVersion % Test
  }

  val playDependencies: Seq[ModuleID] = Seq(
    PlayFrameWork.cache,
    PlayFrameWork.ws,
    PlayFrameWork.guice,
    PlayFrameWork.json,
    PlayFrameWork.jsonJoda,
    JodaTime,
    JodaConvert,
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
