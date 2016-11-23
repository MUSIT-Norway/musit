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

import models.BarcodeFormats._
import play.api.Logger

/**
 * Generator for 1D codes like Code 39, Code 93 and Code 128.
 */
object Barcode1DGenerator extends Generator {

  private val logger = Logger(this.getClass)

  override val defaultWidth = 256
  override lazy val defaultHeight = 50

  /**
   * Generates a Code 39 barcode for an UUID
   *
   * @param value to encode as a barcode
   */
  def writeCode39(value: String): Option[Array[Byte]] = generate1DCode(value, Code39)

  /**
   * Generates a Code 93 barcode for an UUID
   *
   * @param value to encode as a barcode
   */
  def writeCode93(value: String): Option[Array[Byte]] = generate1DCode(value, Code93)

  /**
   * Generates a Code 128 barcode for an UUID
   *
   * @param value to encode as a barcode
   */
  def writeCode128(value: String): Option[Array[Byte]] = generate1DCode(value, Code128)

}
