package no.uio.musit.test

import play.api.inject.guice.GuiceApplicationBuilder

trait MusitFakeApplication extends TestConfigs {

  def createApplication() = {
    if (notExecutedFromPlayProject) {
      setProperty("config.file", "/application.test.conf")
      setProperty("logger.file", "/logback-test.xml")
    }
    new GuiceApplicationBuilder().configure(slickWithInMemoryH2()).build()
  }

  private def notExecutedFromPlayProject =
    System.getProperty("config.file") == null

  private def setProperty(p: String, r: String): Unit = {
    val resource = this.getClass.getResource(r)
    if (resource != null) System.setProperty(p, resource.getPath)
  }

}
