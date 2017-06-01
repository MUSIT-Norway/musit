package models

import models.PageFormats.PageFormat

object TemplateConfigs {

  val AvailableConfigs: Seq[TemplateConfig] = Seq(
    Label1,
    Label2,
    Label3,
    Label4,
    Label5,
    BradyLabelPrinter,
    TSCLabelPrinter
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
        case Label1.templateId            => Some(Label1)
        case Label2.templateId            => Some(Label2)
        case Label3.templateId            => Some(Label3)
        case Label4.templateId            => Some(Label4)
        case Label5.templateId            => Some(Label5)
        case BradyLabelPrinter.templateId => Some(BradyLabelPrinter)
        case TSCLabelPrinter.templateId   => Some(TSCLabelPrinter)
        case _                            => None
      }
    }
  }

  case object Label1 extends TemplateConfig {
    override val templateId: Int          = 1
    override val name: String             = "70x37mm (3x8, A4)"
    override val labelWidth: Millimeters  = Millimeters(70)
    override val labelHeight: Millimeters = Millimeters(37)
    override val rowsPerPage: Int         = 8
    override val colsPerPage: Int         = 3
  }

  case object Label2 extends TemplateConfig {
    override val templateId: Int          = 2
    override val name: String             = "105x74mm, liggende"
    override val labelWidth: Millimeters  = Millimeters(105)
    override val labelHeight: Millimeters = Millimeters(74)
    override val rowsPerPage: Int         = 4
    override val colsPerPage: Int         = 2
  }

  case object Label3 extends TemplateConfig {
    override val templateId: Int          = 3
    override val name: String             = "74x105mm, stående"
    override val labelWidth: Millimeters  = Millimeters(74)
    override val labelHeight: Millimeters = Millimeters(105)
    override val rowsPerPage: Int         = 2
    override val colsPerPage: Int         = 4
  }

  case object Label4 extends TemplateConfig {
    override val templateId: Int          = 4
    override val name: String             = "Hylleetikett 11mm"
    override val labelWidth: Millimeters  = Millimeters(210)
    override val labelHeight: Millimeters = Millimeters(11)
    override val rowsPerPage: Int         = 27
    override val colsPerPage: Int         = 1
  }

  case object Label5 extends TemplateConfig {
    override val templateId: Int          = 5
    override val name: String             = "Hylleetikett 27mm"
    override val labelWidth: Millimeters  = Millimeters(210)
    override val labelHeight: Millimeters = Millimeters(27)
    override val rowsPerPage: Int         = 11
    override val colsPerPage: Int         = 1
  }

  case object BradyLabelPrinter extends TemplateConfig {
    override val templateId: Int          = 6
    override val name: String             = "BRADY BBP33 (52x27mm)"
    override val labelWidth: Millimeters  = Millimeters(52)
    override val labelHeight: Millimeters = Millimeters(26)
    override val rowsPerPage: Int         = 1
    override val colsPerPage: Int         = 1
  }

  case object TSCLabelPrinter extends TemplateConfig {
    override val templateId: Int          = 7
    override val name: String             = "TSC ME340 (54x27mm)"
    override val labelWidth: Millimeters  = Millimeters(52)
    override val labelHeight: Millimeters = Millimeters(26)
    override val rowsPerPage: Int         = 1
    override val colsPerPage: Int         = 1
  }
}
