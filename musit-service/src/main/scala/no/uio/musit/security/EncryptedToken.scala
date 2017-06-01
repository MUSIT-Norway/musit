package no.uio.musit.security

import no.uio.musit.security.crypto.MusitCrypto

case class EncryptedToken(underlying: String) extends AnyVal {

  def asString = underlying

  def urlEncoded = java.net.URLEncoder.encode(underlying, "utf-8")

}

object EncryptedToken {

  def fromBearerToken(bt: BearerToken)(implicit crypto: MusitCrypto) = {
    EncryptedToken(crypto.encryptAES(bt.underlying))
  }

}
