package no.uio.musit.security.crypto

class CryptoException(
    val message: String = null,
    val throwable: Throwable = null
) extends RuntimeException(message, throwable)
