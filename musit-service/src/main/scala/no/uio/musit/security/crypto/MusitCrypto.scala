package no.uio.musit.security.crypto

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}

import com.google.inject.{Inject, Singleton}
import org.apache.commons.codec.binary.Base64
import play.api.Logger
import play.api.libs.Codecs
import play.api.libs.crypto.CryptoException

/**
 * This class contains a copy of the deprecated implementation of
 * {{{play.api.libs.crypto.AESCTRCrypter}}}. The code is duplicated because we
 * need symmetric encryption for a specific use-case in the admin client.
 *
 * When time allows, the admin client should be extracted as a separate app,
 * and modified to use the REST endpoints to load data.
 */
@Singleton
class MusitCrypto @Inject()(ccp: CryptoConfigParser) {

  val logger = Logger(classOf[MusitCrypto])

  lazy val config = ccp.get

  private val cipher = Cipher.getInstance(config.aesTransformation)

  def encryptAES(value: String): String = {
    encryptAES(value, config.secret)
  }

  def encryptAES(value: String, privateKey: String): String = {
    logger.trace(s"Encrypting value: $value")
    val skeySpec = secretKeyWithSha256(privateKey, "AES")
    cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
    val encryptedValue = cipher.doFinal(value.getBytes("utf-8"))
    // return a formatted, versioned encrypted string
    // '2-*' represents an encrypted payload with an IV
    // '1-*' represents an encrypted payload without an IV
    logger.trace(s"Using IV ${Option(cipher.getIV).map(_.mkString)}")
    val enc = Option(cipher.getIV) match {
      case Some(iv) => s"2-${Base64.encodeBase64String(iv ++ encryptedValue)}"
      case None     => s"1-${Base64.encodeBase64String(encryptedValue)}"
    }
    logger.trace(s"Encrypted value is $enc")
    enc
  }

  /**
   * Generates the SecretKeySpec, given the private key and the algorithm.
   */
  private def secretKeyWithSha256(privateKey: String, algorithm: String) = {
    val messageDigest = MessageDigest.getInstance("SHA-256")
    messageDigest.update(privateKey.getBytes("utf-8"))
    // max allowed length in bits / (8 bits to a byte)
    val maxAllowedKeyLength = Cipher.getMaxAllowedKeyLength(algorithm) / 8
    val raw                 = messageDigest.digest().slice(0, maxAllowedKeyLength)
    new SecretKeySpec(raw, algorithm)
  }

  def decryptAES(value: String): String = {
    decryptAES(value, config.secret)
  }

  def decryptAES(value: String, privateKey: String): String = {
    logger.trace(s"Decrypting value: $value")
    val seperator = "-"
    val sepIndex  = value.indexOf(seperator)
    if (sepIndex < 0) {
      logger.trace(s"Using AES V0 decryption")
      decryptAESVersion0(value, privateKey)
    } else {
      val version = value.substring(0, sepIndex)
      val data    = value.substring(sepIndex + 1, value.length())
      val dec = version match {
        case "1" => decryptAESVersion1(data, privateKey)
        case "2" => decryptAESVersion2(data, privateKey)
        case _   => throw new CryptoException("Unknown version")
      }
      logger.trace(s"Decrypted Using AES V$version.")
      logger.trace(s"Got decrypted value: $dec")
      dec
    }
  }

  /** Backward compatible AES ECB mode decryption support. */
  private def decryptAESVersion0(value: String, privateKey: String): String = {
    val raw      = privateKey.substring(0, 16).getBytes("utf-8")
    val skeySpec = new SecretKeySpec(raw, "AES")
    cipher.init(Cipher.DECRYPT_MODE, skeySpec)
    new String(cipher.doFinal(Codecs.hexStringToByte(value)))
  }

  /** V1 decryption algorithm (No IV). */
  private def decryptAESVersion1(value: String, privateKey: String): String = {
    val data     = Base64.decodeBase64(value)
    val skeySpec = secretKeyWithSha256(privateKey, "AES")
    cipher.init(Cipher.DECRYPT_MODE, skeySpec)
    new String(cipher.doFinal(data), "utf-8")
  }

  /** V2 decryption algorithm (IV present). */
  private def decryptAESVersion2(value: String, privateKey: String): String = {
    val data      = Base64.decodeBase64(value)
    val skeySpec  = secretKeyWithSha256(privateKey, "AES")
    val blockSize = cipher.getBlockSize
    val iv        = data.slice(0, blockSize)
    val payload   = data.slice(blockSize, data.size)
    cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(iv))
    new String(cipher.doFinal(payload), "utf-8")
  }

}
