package no.uio.musit.test

import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

trait MusitFakeApplication extends TestConfigs {

  def createApplication(): Application = {
    if (notExecutedFromPlayProject) {
      setPropertyIfFileExists("config.file", "/application.test.conf")
      setPropertyIfFileExists("logger.file", "/logback-test.xml")
    }
    new GuiceApplicationBuilder().configure(slickWithInMemoryH2()).build()
  }

  private def notExecutedFromPlayProject =
    sys.props.get("config.file").isEmpty

  private def setPropertyIfFileExists(p: String, r: String): Unit =
    Option(this.getClass.getResource(r))
      .foreach(res => System.setProperty(p, res.getPath))

}
