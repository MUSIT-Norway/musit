package no.uio.musit.security.fake

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import no.uio.musit.security.Authenticator

// $COVERAGE-OFF$
class FakeModule extends AbstractModule with ScalaModule {

  def configure(): Unit = {
    bind[Authenticator].to[FakeAuthenticator]
  }

}
// $COVERAGE-ON$
