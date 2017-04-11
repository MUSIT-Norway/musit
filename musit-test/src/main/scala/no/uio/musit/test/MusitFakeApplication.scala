package no.uio.musit.test

import play.api.inject.guice.GuiceApplicationBuilder

trait MusitFakeApplication extends TestConfigs {

  def createApplication() = {
    if (notExecutedFromSbt) {
      setProperty("config.file", "/application.test.conf")
      setProperty("logger.file", "/logback-test.xml")
    }
    new GuiceApplicationBuilder().configure(slickWithInMemoryH2()).build()
  }

  private def notExecutedFromSbt = System.getProperty("config.file") == null

  private def setProperty(p: String, r: String): Unit =
    System.setProperty(p, this.getClass.getResource(r).getPath)

}
