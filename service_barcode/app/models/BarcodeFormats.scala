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

package models

import com.google.zxing.{BarcodeFormat => ZxingFormat}
import play.api.Logger

object BarcodeFormats {

  sealed trait BarcodeFormat {
    val code: Int
    val shouldUpperCase: Boolean = false
    val zxingFormat: ZxingFormat
  }

  object BarcodeFormat {

    private val logger = Logger(classOf[BarcodeFormat])

    def fromInt(i: Int): Option[BarcodeFormat] = {
      i match {
        case QrCode.code => Some(QrCode)
        case DataMatrix.code => Some(DataMatrix)
//        case Code39.code => Code39
//        case Code93.code => Code93
//        case Code128.code => Code128
        case _ =>
          logger.warn(s"Barcode format $i is currently not supported")
          None
      }
    }

  }

  sealed trait BarcodeFormat2D extends BarcodeFormat

  sealed trait BarcodeFormat1D extends BarcodeFormat

  case object QrCode extends BarcodeFormat2D {
    override val code: Int = 1
    override val zxingFormat: ZxingFormat = ZxingFormat.QR_CODE
  }

  case object DataMatrix extends BarcodeFormat2D {
    override val code: Int = 2
    override val zxingFormat: ZxingFormat = ZxingFormat.DATA_MATRIX
  }

  case object Code39 extends BarcodeFormat1D {
    override val code: Int = 3
    override val shouldUpperCase: Boolean = true
    override val zxingFormat: ZxingFormat = ZxingFormat.CODE_39
  }

  case object Code93 extends BarcodeFormat1D {
    override val code: Int = 4
    override val shouldUpperCase: Boolean = true
    override val zxingFormat: ZxingFormat = ZxingFormat.CODE_93
  }

  case object Code128 extends BarcodeFormat1D {
    override val code: Int = 5
    override val zxingFormat: ZxingFormat = ZxingFormat.CODE_128
  }

}
