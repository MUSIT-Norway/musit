package no.uio.musit.test

import play.api.inject.guice.GuiceApplicationBuilder

trait MusitFakeApplication extends TestConfigs {

  def createApplication() =
    new GuiceApplicationBuilder().configure(slickWithInMemoryH2()).build()

}
