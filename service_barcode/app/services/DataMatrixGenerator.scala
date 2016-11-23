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
import com.google.zxing.datamatrix.encoder.SymbolShapeHint
import models.BarcodeFormats.DataMatrix
import play.api.Logger

object DataMatrixGenerator extends Generator {

  private val logger = Logger(this.getClass)

  val minimumSize = 22

  override val defaultWidth = 22

  /**
   * Generates a DataMatrix code for an UUID
   *
   * @param value to encode as a DataMatrix code
   * @param width
   * @param height
   * @return An Array[Byte] representing the DataMatrix image
   */
  def writeDataMatrix(
    value: UUID,
    width: Option[Int],
    height: Option[Int]
  ): Option[Array[Byte]] = {
    val hints = Map[EncodeHintType, Any](
      EncodeHintType.DATA_MATRIX_SHAPE -> SymbolShapeHint.FORCE_NONE
    )

    generate2DCode(
      value.toString,
      DataMatrix,
      width.getOrElse(defaultWidth),
      height.getOrElse(defaultHeight),
      hints
    )
  }

}
