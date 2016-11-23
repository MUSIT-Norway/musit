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

package services

import java.util.UUID

import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import models.BarcodeFormats.QrCode
import play.api.Logger

object QrGenerator extends Generator {

  private val logger = Logger(this.getClass)

  override val defaultWidth = 50

  /**
   * Generates a QR code for an UUID
   *
   * @param value to encode as a QR code
   * @param width
   * @param height
   * @return An Array[Byte] representing the DataMatrix image
   */
  override def write(
    value: UUID,
    width: Option[Int],
    height: Option[Int]
  ): Option[Array[Byte]] = {
    val hints = Map[EncodeHintType, Any](
      EncodeHintType.ERROR_CORRECTION -> ErrorCorrectionLevel.L
    )
    generate2DCode(
      value.toString,
      QrCode,
      width.getOrElse(defaultWidth),
      height.getOrElse(defaultHeight),
      hints
    )
  }

}
