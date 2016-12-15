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

  val AvailableConfigs: Seq[TemplateConfig] = Seq(
    Label1,
    Label2,
    Label3,
    Avery5160,
    Herma9650
  )

  sealed trait TemplateConfig {
    val templateId: Int
    val name: String
    val labelWidth: Millimeters
    val labelHeight: Millimeters
    val rowsPerPage: Int
    val colsPerPage: Int

    val pageFormat: PageFormat = PageFormats.A4

    def numPerPage = rowsPerPage * colsPerPage

  }

  object TemplateConfig {
    def fromInt(i: Int): Option[TemplateConfig] = {
      i match {
        case Label1.templateId => Some(Label1)
        case Label2.templateId => Some(Label2)
        case Label3.templateId => Some(Label3)
        case Avery5160.templateId => Some(Avery5160)
        case Herma9650.templateId => Some(Herma9650)
        case _ => None
      }
    }
  }

  case object Label1 extends TemplateConfig {
    override val templateId: Int = 3
    override val name: String = "Label-1 70x37"
    override val labelWidth: Millimeters = Millimeters(70)
    override val labelHeight: Millimeters = Millimeters(37)
    override val rowsPerPage: Int = 8
    override val colsPerPage: Int = 3
  }

  case object Label2 extends TemplateConfig {
    override val templateId: Int = 4
    override val name: String = "Label-2 105x74"
    override val labelWidth: Millimeters = Millimeters(105)
    override val labelHeight: Millimeters = Millimeters(74)
    override val rowsPerPage: Int = 4
    override val colsPerPage: Int = 2
  }

  case object Label3 extends TemplateConfig {
    override val templateId: Int = 5
    override val name: String = "Label-2 74x105"
    override val labelWidth: Millimeters = Millimeters(74)
    override val labelHeight: Millimeters = Millimeters(105)
    override val rowsPerPage: Int = 2
    override val colsPerPage: Int = 4
  }

  case object Avery5160 extends TemplateConfig {
    override val templateId: Int = 1
    override val name: String = "Avery 51x60"
    override val labelWidth: Millimeters = Millimeters(25.198)
    override val labelHeight: Millimeters = Millimeters(96)
    override val rowsPerPage: Int = 30
    override val colsPerPage: Int = 3
  }

  case object Herma9650 extends TemplateConfig {
    override val templateId: Int = 2
    override val name: String = "Herma 95x50.8"
    override val labelWidth: Millimeters = Millimeters(37.792)
    override val labelHeight: Millimeters = Millimeters(20.709)
    override val rowsPerPage: Int = 5
    override val colsPerPage: Int = 2
  }

}
