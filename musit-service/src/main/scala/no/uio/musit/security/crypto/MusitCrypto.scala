/*
 * MUSIT is a museum database to archive natural and cultural history data.
 * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package no.uio.musit.security.crypto

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}

import com.google.inject.{Inject, Singleton}
import org.apache.commons.codec.binary.Base64
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
class MusitCrypto @Inject() (
    val ccp: CryptoConfigParser
) {

  lazy val config = ccp.get

  /**
   * Gets a Cipher with a configured provider, and a configurable AES
   * transformation method.
   */
  private def getCipherWithConfiguredProvider(transformation: String): Cipher = {
    config.provider.fold(Cipher.getInstance(transformation)) { p =>
      Cipher.getInstance(transformation, p)
    }
  }

  def encryptAES(value: String): String = {
    encryptAES(value, config.secret)
  }

  def encryptAES(value: String, privateKey: String): String = {
    val skeySpec = secretKeyWithSha256(privateKey, "AES")
    val cipher = getCipherWithConfiguredProvider(config.aesTransformation)
    cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
    val encryptedValue = cipher.doFinal(value.getBytes("utf-8"))
    // return a formatted, versioned encrypted string
    // '2-*' represents an encrypted payload with an IV
    // '1-*' represents an encrypted payload without an IV
    Option(cipher.getIV) match {
      case Some(iv) => s"2-${Base64.encodeBase64String(iv ++ encryptedValue)}"
      case None => s"1-${Base64.encodeBase64String(encryptedValue)}"
    }
  }

  /**
   * Generates the SecretKeySpec, given the private key and the algorithm.
   */
  private def secretKeyWithSha256(privateKey: String, algorithm: String) = {
    val messageDigest = MessageDigest.getInstance("SHA-256")
    messageDigest.update(privateKey.getBytes("utf-8"))
    // max allowed length in bits / (8 bits to a byte)
    val maxAllowedKeyLength = Cipher.getMaxAllowedKeyLength(algorithm) / 8
    val raw = messageDigest.digest().slice(0, maxAllowedKeyLength)
    new SecretKeySpec(raw, algorithm)
  }

  def decryptAES(value: String): String = {
    decryptAES(value, config.secret)
  }

  def decryptAES(value: String, privateKey: String): String = {
    val seperator = "-"
    val sepIndex = value.indexOf(seperator)
    if (sepIndex < 0) {
      decryptAESVersion0(value, privateKey)
    } else {
      val version = value.substring(0, sepIndex)
      val data = value.substring(sepIndex + 1, value.length())
      version match {
        case "1" => {
          decryptAESVersion1(data, privateKey)
        }
        case "2" => {
          decryptAESVersion2(data, privateKey)
        }
        case _ => {
          throw new CryptoException("Unknown version")
        }
      }
    }
  }

  /** Backward compatible AES ECB mode decryption support. */
  private def decryptAESVersion0(value: String, privateKey: String): String = {
    val raw = privateKey.substring(0, 16).getBytes("utf-8")
    val skeySpec = new SecretKeySpec(raw, "AES")
    val cipher = getCipherWithConfiguredProvider("AES")
    cipher.init(Cipher.DECRYPT_MODE, skeySpec)
    new String(cipher.doFinal(Codecs.hexStringToByte(value)))
  }

  /** V1 decryption algorithm (No IV). */
  private def decryptAESVersion1(value: String, privateKey: String): String = {
    val data = Base64.decodeBase64(value)
    val skeySpec = secretKeyWithSha256(privateKey, "AES")
    val cipher = getCipherWithConfiguredProvider(config.aesTransformation)
    cipher.init(Cipher.DECRYPT_MODE, skeySpec)
    new String(cipher.doFinal(data), "utf-8")
  }

  /** V2 decryption algorithm (IV present). */
  private def decryptAESVersion2(value: String, privateKey: String): String = {
    val data = Base64.decodeBase64(value)
    val skeySpec = secretKeyWithSha256(privateKey, "AES")
    val cipher = getCipherWithConfiguredProvider(config.aesTransformation)
    val blockSize = cipher.getBlockSize
    val iv = data.slice(0, blockSize)
    val payload = data.slice(blockSize, data.size)
    cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(iv))
    new String(cipher.doFinal(payload), "utf-8")
  }

}
