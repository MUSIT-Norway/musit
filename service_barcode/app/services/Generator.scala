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

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.imageio.ImageIO

import com.google.zxing.{EncodeHintType, MultiFormatWriter}
import models.BarcodeFormats._
import play.api.Logger

import scala.util.Try

trait Generator {
  self =>

  private val logger = Logger(this.getClass)

  protected val white: Int = 0xFFFFFFFF
  protected val black: Int = 0xFF000000

  val defaultWidth: Int
  lazy val defaultHeight: Int = defaultWidth

  /**
   * Generate the actual barcode/qr code image and return it as an Array[Byte].
   * The images are so small that the footprint of keeping the in-memory should
   * not be a problem.
   */
  protected def generate(
    value: String,
    format: BarcodeFormat,
    width: Int,
    height: Int,
    hints: Map[EncodeHintType, Any] = Map.empty
  ): Try[Array[Byte]] = Try {
    val writer = new MultiFormatWriter
    import scala.collection.JavaConversions._
    // encode the value and ensure that it has correct case for the format.
    val v = if (format.shouldUpperCase) value.toUpperCase else value.toLowerCase
    val matrix = writer.encode(v, format.zxingFormat, width, height, hints)

    // "draw" the pixels (as black or white)
    val pixels = Array.newBuilder[Int]
    for (i <- 0 until height) {
      for (j <- 0 until width) {
        val blackOrWhite = if (matrix.get(j, i)) black else white
        pixels += blackOrWhite
      }
    }

    // create an empty image
    val img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    // Add the pixels to the image
    img.setRGB(0, 0, width, height, pixels.result(), 0, width)

    val baos = new ByteArrayOutputStream()
    ImageIO.write(img, "png", baos)
    baos.toByteArray
  }.recover {
    case ex: Throwable =>
      logger.warn(s"Unable to generate ${format.zxingFormat.name()}", ex)
      throw ex // scalastyle:ignore
  }

  /**
   * Generates any of the supported 2D codes (QR or DataMatrix).
   */
  def generate2DCode(
    value: String,
    format: BarcodeFormat2D,
    width: Int = defaultWidth,
    height: Int = defaultHeight,
    hints: Map[EncodeHintType, Any] = Map.empty
  ): Option[Array[Byte]] = generate(value, format, width, height, hints).toOption

  /**
   * Generates any of the supported 1D bar codes.
   */
  def generate1DCode(
    value: String,
    format: BarcodeFormat1D,
    width: Int = defaultWidth,
    height: Int = defaultHeight
  ): Option[Array[Byte]] = {
    val hints = Map(EncodeHintType.MARGIN -> 20)
    generate(value, format, width, height, hints).toOption
  }

  /**
   * Generates an image of a barcode format based on the given input.
   *
   * @param value The UUID to generate an encoded image for
   * @param width The width dimension of the image
   * @param height The height dimension of the image
   *
   * @return An Array[Byte] representing the barcode image
   */
  def write(value: UUID, width: Option[Int], height: Option[Int]): Option[Array[Byte]]

}

object Generator {

  def generatorFor(bf: BarcodeFormat): Option[Generator] = {
    bf match {
      case QrCode => Some(QrGenerator)
      case DataMatrix => Some(DataMatrixGenerator)
      case _ => None
    }
  }

}
