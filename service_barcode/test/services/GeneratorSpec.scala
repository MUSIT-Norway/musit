package services

import java.io.InputStream
import java.util.UUID
import javax.imageio.ImageIO

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.StreamConverters
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.datamatrix.DataMatrixReader
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.{BinaryBitmap, DecodeHintType}
import models.BarcodeFormats.{DataMatrix, QrCode}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}

import scala.collection.JavaConverters._

class GeneratorSpec extends WordSpec with OptionValues with MustMatchers {

  implicit val as  = ActorSystem("test")
  implicit val mat = ActorMaterializer.create(as)

  val hints = Map(DecodeHintType.PURE_BARCODE -> true).asJava

  "Generator DataMatrix" should {
    "generate and parse UUID" in {
      val content = UUID.randomUUID()
      val image = Generator
        .generatorFor(DataMatrix)
        .flatMap(_.write(content))
        .map(_.runWith(StreamConverters.asInputStream()))
        .map(streamToBitmap)
        .value

      val res = (new DataMatrixReader).decode(image, hints)

      res.getText mustBe content.toString
    }

    "not cast FormatException" in {
      val content = UUID.fromString("6c2a5518-c0c6-4b2e-ace3-40a5abd57a15")

      val image = Generator
        .generatorFor(DataMatrix)
        .flatMap(_.write(content))
        .map(_.runWith(StreamConverters.asInputStream()))
        .map(streamToBitmap)
        .value

      val res = (new DataMatrixReader).decode(image, hints)

      res.getText mustBe content.toString
    }

    "not cast ChecksumException" in {
      val content = UUID.fromString("ffb42bb8-fee7-48fe-8eb0-d3f815f45644")

      val image = Generator
        .generatorFor(DataMatrix)
        .flatMap(_.write(content))
        .map(_.runWith(StreamConverters.asInputStream()))
        .map(streamToBitmap)
        .value

      val res = (new DataMatrixReader).decode(image, hints)

      res.getText mustBe content.toString
    }
  }

  "Generator Qr-Code" should {
    "generate and parse UUID from QR code" in {
      val content = UUID.randomUUID()

      val image = Generator
        .generatorFor(QrCode)
        .flatMap(_.write(content))
        .map(_.runWith(StreamConverters.asInputStream()))
        .map(streamToBitmap)
        .value

      val res = (new QRCodeReader).decode(image, hints)

      res.getText mustBe content.toString
    }
  }

  private def streamToBitmap(is: InputStream) = {
    val img = ImageIO.read(is)
    new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(img)))
  }
}
