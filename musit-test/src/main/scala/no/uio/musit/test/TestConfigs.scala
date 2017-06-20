package no.uio.musit.test

import scala.util.Random

trait TestConfigs {

  def slickWithInMemoryH2(
      evolve: String = "enabled"
  ): Map[String, Any] = Map(
    "play.evolutions.db.default.enabled"    -> false,
    "play.evolutions.db.default.autoApply"  -> false,
    "play.slick.db.default"                 -> "test",
    "slick.dbs.test.driver"                 -> "slick.driver.H2Driver$",
    "slick.dbs.test.connectionTimeout"      -> "30000",
    "slick.dbs.test.loginTimeout"           -> "30000",
    "slick.dbs.test.socketTimeout"          -> "30000",
    "slick.dbs.test.db.driver"              -> "org.h2.Driver",
    "slick.dbs.test.connectionTestQuery"    -> "SELECT 1",
    "slick.dbs.test.db.url"                 -> s"jdbc:h2:mem:musit-test${Random.nextInt()};MODE=Oracle;DB_CLOSE_DELAY=-1", // scalastyle:ignore
    "slick.dbs.test.leakDetectionThreshold" -> "5000",
    "evolutionplugin"                       -> evolve
  )

}

object TestConfigs extends TestConfigs
