package services

import java.util.UUID

import akka.stream.IOResult
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import models.BarcodeFormats.QrCode
import play.api.Logger

import scala.concurrent.Future

object QrGenerator extends Generator {

  private val logger = Logger(this.getClass)

  override val width = 100

  /**
   * Generates a QR code for an UUID
   *
   * @param value to encode as a QR code
   * @return A Source representing the DataMatrix image
   */
  override def write(value: UUID): Option[Source[ByteString, Future[IOResult]]] = {
    val hints = Map[EncodeHintType, Any](
      EncodeHintType.ERROR_CORRECTION -> ErrorCorrectionLevel.H
    )
    generate(value.toString, QrCode, hints).toOption
  }

}
