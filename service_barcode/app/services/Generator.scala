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

trait Generator { self =>

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
  ): Try[Source[ByteString, Future[IOResult]]] =
    Try {
      val writer = new MultiFormatWriter
      // We need to implicitly convert hints from Scala to Java Map before encoding
      import scala.collection.JavaConversions._
      val matrix    = writer.encode(value, format.zxingFormat, width, height, hints)
      val imgHeight = matrix.getHeight
      val imgWidth  = matrix.getWidth

      logger.debug(s"matrix height: ${matrix.getHeight}  width: ${matrix.getWidth}")

      // "draw" the pixels (as black or white)
      val pixels = Array.newBuilder[Int]
      for (y <- 0 until imgHeight) {
        for (x <- 0 until imgWidth) {
          val blackOrWhite = if (matrix.get(x, y)) black else white
          pixels += blackOrWhite
        }
      }

      // create an empty image
      val img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB)
      // Add the pixels to the image
      img.setRGB(0, 0, imgWidth, imgHeight, pixels.result(), 0, imgWidth)
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
      case QrCode     => Some(QrGenerator)
      case DataMatrix => Some(DataMatrixGenerator)
      case _          => None
    }
  }

}
