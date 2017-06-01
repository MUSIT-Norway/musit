package models

import com.google.zxing.{BarcodeFormat => ZxingFormat}
import play.api.Logger

object BarcodeFormats {

  sealed trait BarcodeFormat {
    val code: Int
    val zxingFormat: ZxingFormat
  }

  object BarcodeFormat {

    private val logger = Logger(classOf[BarcodeFormat])

    def fromInt(i: Int): Option[BarcodeFormat] = {
      i match {
        case QrCode.code     => Some(QrCode)
        case DataMatrix.code => Some(DataMatrix)
        case _ =>
          logger.warn(s"Barcode format $i is currently not supported")
          None
      }
    }

  }

  case object QrCode extends BarcodeFormat {
    override val code: Int                = 1
    override val zxingFormat: ZxingFormat = ZxingFormat.QR_CODE
  }

  case object DataMatrix extends BarcodeFormat {
    override val code: Int                = 2
    override val zxingFormat: ZxingFormat = ZxingFormat.DATA_MATRIX
  }

}
