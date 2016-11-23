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

package models

import models.PageFormats.PageFormat

object TemplateConfigs {

  sealed trait TemplateConfig {
    val templateId: Int
    val labelWidth: Millimeters
    val labelHeight: Millimeters

    val pageFormat: PageFormat = PageFormats.A4

    lazy val rowsPerPage: Int = (pageFormat.height / labelHeight).toInt
    lazy val colsPerPage: Int = (pageFormat.width / labelWidth).toInt
  }

  object TemplateConfig {
    def fromInt(i: Int): Option[TemplateConfig] = {
      i match {
        case Avery5160.templateId => Some(Avery5160)
        case Herma9560.templateId => Some(Herma9560)
        case _ => None
      }
    }
  }

  case object Avery5160 extends TemplateConfig {
    override val templateId: Int = 1
    override val labelWidth: Millimeters = Millimeters(51.453)
    override val labelHeight: Millimeters = Millimeters(22.225)
  }

  case object Herma9560 extends TemplateConfig {
    override val templateId: Int = 2
    override val labelWidth: Millimeters = Millimeters(96D)
    override val labelHeight: Millimeters = Millimeters(50.8)
  }

}
