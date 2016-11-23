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

import java.awt.image.BufferedImage // scalastyle:ignore
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.UUID
import javax.imageio.ImageIO

import akka.stream.IOResult
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import com.google.zxing.{EncodeHintType, MultiFormatWriter}
import models.BarcodeFormats._
import play.api.Logger

import scala.concurrent.Future
import scala.util.Try

trait Generator {
  self =>

  private val logger = Logger(this.getClass)

  protected val white: Int = 0xFFFFFFFF
  protected val black: Int = 0xFF000000

  val width: Int
  lazy val height: Int = width

  /**
   * Generate the actual barcode/qr code image and return it as a Source.
   * The images are so small that the footprint of keeping the in-memory should
   * not be a problem.
   */
  protected def generate(
    value: String,
    format: BarcodeFormat,
    hints: Map[EncodeHintType, Any] = Map.empty
  ): Try[Source[ByteString, Future[IOResult]]] = Try {
    val writer = new MultiFormatWriter
    // We need to implicitly convert hints from Scala to Java Map before encoding
    import scala.collection.JavaConversions._
    val matrix = writer.encode(value, format.zxingFormat, width, height, hints)

    // "draw" the pixels (as black or white)
    val pixels = Array.newBuilder[Int]
    for (i <- 0 until width) {
      for (j <- 0 until height) {
        val blackOrWhite = if (matrix.get(i, j)) black else white
        pixels += blackOrWhite
      }
    }

    // create an empty image
    val img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    // Add the pixels to the image
    img.setRGB(0, 0, width, height, pixels.result(), 0, width)

    val baos = new ByteArrayOutputStream()
    ImageIO.write(img, "png", baos)
    StreamConverters.fromInputStream(() => new ByteArrayInputStream(baos.toByteArray))
  }.recover {
    case ex: Throwable =>
      logger.warn(s"Unable to generate ${format.zxingFormat.name()}", ex)
      throw ex // scalastyle:ignore
  }

  /**
   * Generates an image of a barcode format based on the given input.
   *
   * @param value The UUID to generate an encoded image for
   * @return A Source representing the barcode image
   */
  def write(value: UUID): Option[Source[ByteString, Future[IOResult]]]

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
