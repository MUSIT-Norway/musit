package models

object PageFormats {

  sealed trait PageFormat {
    val width: Millimeters
    val height: Millimeters
  }

  case object A4 extends PageFormat {
    override val width  = Millimeters(210D)
    override val height = Millimeters(297D)
  }

}
