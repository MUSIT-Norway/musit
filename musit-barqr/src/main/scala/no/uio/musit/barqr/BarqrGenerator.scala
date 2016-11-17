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

package no.uio.musit.barqr

import java.awt.image.BufferedImage // scalastyle:ignore

import com.google.zxing.{BarcodeFormat, EncodeHintType, MultiFormatWriter}

import scala.collection.JavaConversions._

object BarqrGenerator {

  private[this] val white: Int = 0xFFFFFFFF
  private[this] val black: Int = 0xFF000000

  private[this] val defaultHeight = 256
  private[this] val defaultWidth = defaultHeight


  private[this] def generateCode(
    value: String,
    format: BarcodeFormat,
    fileName: String,
    width: Int = defaultWidth,
    height: Int = defaultHeight,
    hints: Map[EncodeHintType, Any] = Map.empty
  ) = {
    val writer = new MultiFormatWriter
    // encode the value
    val matrix = writer.encode(value, format, width, height, hints)
    // create an empty image
    val img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

    val pixels = Array.newBuilder[Int]

    for (i <- 0 until height) {
      for (j <- 0 until width) {
        val blackOrWhite = if (matrix.get(j, i)) black else white
      }
    }
  }

  // 2d codes

  /**
   * Generates a QR code for an UUID
   *
   * @param value to encode as a QR code
   */
  def writeQR(value: String) = {}

  /**
   * Generates a Datamatrix code for an UUID
   *
   * @param value to encode as a Datamatrix code
   */
  def writeDatamatrix(value: String) = {}


  // 1d codes
  /**
   * Generates a Code 39 barcode for an UUID
   *
   * @param value to encode as a barcode
   */
  def writeCode39(value: String) = {}

  /**
   * Generates a Code 128 barcode for an UUID
   *
   * @param value to encode as a barcode
   */
  def writeCode128(value: String) = {}

}
