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
    Label4,
    Label5
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
        case Label4.templateId => Some(Label4)
        case Label5.templateId => Some(Label5)
        case _ => None
      }
    }
  }

  case object Label1 extends TemplateConfig {
    override val templateId: Int = 1
    override val name: String = "Label-1 70mm x 37mm"
    override val labelWidth: Millimeters = Millimeters(70)
    override val labelHeight: Millimeters = Millimeters(37)
    override val rowsPerPage: Int = 8
    override val colsPerPage: Int = 3
  }

  case object Label2 extends TemplateConfig {
    override val templateId: Int = 2
    override val name: String = "Label-2 105mm x 74mm"
    override val labelWidth: Millimeters = Millimeters(105)
    override val labelHeight: Millimeters = Millimeters(74)
    override val rowsPerPage: Int = 4
    override val colsPerPage: Int = 2
  }

  case object Label3 extends TemplateConfig {
    override val templateId: Int = 3
    override val name: String = "Label-3 74mm x 105mm"
    override val labelWidth: Millimeters = Millimeters(74)
    override val labelHeight: Millimeters = Millimeters(105)
    override val rowsPerPage: Int = 2
    override val colsPerPage: Int = 4
  }

  case object Label4 extends TemplateConfig {
    override val templateId: Int = 4
    override val name: String = "Label-4 210mm x 11mm"
    override val labelWidth: Millimeters = Millimeters(210)
    override val labelHeight: Millimeters = Millimeters(11)
    override val rowsPerPage: Int = 27
    override val colsPerPage: Int = 1
  }

  case object Label5 extends TemplateConfig {
    override val templateId: Int = 5
    override val name: String = "Label-5 210 x 27mm"
    override val labelWidth: Millimeters = Millimeters(210)
    override val labelHeight: Millimeters = Millimeters(27)
    override val rowsPerPage: Int = 11
    override val colsPerPage: Int = 1
  }

}
