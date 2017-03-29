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
import com.google.zxing.{BinaryBitmap, DecodeHintType}
import models.BarcodeFormats.DataMatrix
import org.scalatest.{MustMatchers, OptionValues, WordSpec}

import scala.collection.JavaConverters._

class GeneratorSpec extends WordSpec with OptionValues with MustMatchers {

  "Generator" should {
    implicit val as  = ActorSystem("test")
    implicit val mat = ActorMaterializer.create(as)

    val reader = new DataMatrixReader
    val hints  = Map(DecodeHintType.PURE_BARCODE -> true).asJava

    "generate and parse UUID from Data Matrix code" in {
      val content = UUID.randomUUID()

      val image = Generator
        .generatorFor(DataMatrix)
        .flatMap(_.write(content))
        .map(_.runWith(StreamConverters.asInputStream()))
        .map(streamToBitmap)
        .value

      val res = reader.decode(image, hints)

      res.getText mustBe content.toString
    }
  }

  private def streamToBitmap(is: InputStream) = {
    val img = ImageIO.read(is)
    new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(img)))
  }
}
