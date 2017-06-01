package no.uio.musit.security.dataporten

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import no.uio.musit.security.{AuthResolver, Authenticator}

// $COVERAGE-OFF$
class DataportenModule extends AbstractModule with ScalaModule {

  def configure(): Unit = {
    bind[Authenticator].to[DataportenAuthenticator]
    bind[AuthResolver].to[DatabaseAuthResolver]
  }

}
// $COVERAGE-ON$
