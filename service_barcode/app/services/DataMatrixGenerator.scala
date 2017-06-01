package services

import java.util.UUID

import akka.stream.IOResult
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.google.zxing.EncodeHintType
import com.google.zxing.datamatrix.encoder.SymbolShapeHint
import models.BarcodeFormats.DataMatrix
import play.api.Logger

import scala.concurrent.Future

object DataMatrixGenerator extends Generator {

  private val logger = Logger(this.getClass)

  val minimumSize = 22

  override val width = 22

  /**
   * Generates a DataMatrix code for an UUID
   *
   * @param value to encode as a DataMatrix code
   * @return A Source representing the DataMatrix image
   */
  def write(value: UUID): Option[Source[ByteString, Future[IOResult]]] = {
    val hints = Map[EncodeHintType, Any](
      EncodeHintType.DATA_MATRIX_SHAPE -> SymbolShapeHint.FORCE_SQUARE
    )

    generate(value.toString, DataMatrix, hints).toOption
  }

}
