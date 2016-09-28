package no.uio.musit.security

/**
 * TEMP file, should be included from some common source, not a local copy!
 */
//TODO: Remove this file

case class MuseumId(underlying: Int) extends AnyVal

sealed trait Museum {
  val id: MuseumId
}

object Museum {
  def fromMuseumId(i: MuseumId): Option[Museum] =
    i match {
      case Am.id => Some(Am)
      case Um.id => Some(Um)
      case Khm.id => Some(Khm)
      case Nhm.id => Some(Nhm)
      case Vm.id => Some(Vm)
      case Tmu.id => Some(Tmu)
      case unknown => None
    }
}

case object Am extends Museum {
  val id = MuseumId(1)
}

case object Um extends Museum {
  val id = MuseumId(2)
}

case object Khm extends Museum {
  val id = MuseumId(3)
}

case object Nhm extends Museum {
  val id = MuseumId(4)
}

case object Vm extends Museum {
  val id = MuseumId(5)
}

case object Tmu extends Museum {
  val id = MuseumId(6)
}
