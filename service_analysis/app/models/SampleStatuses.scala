package models

object SampleStatuses {

  sealed trait SampleStatus {
    val identity: Int
  }

  object SampleStatus {

    def fromInt(i: Int): Option[SampleStatus] = {
      i match {
        case Ok.identity => Some(Ok)
        case Destroyed.identity => Some(Destroyed)
        case Contaminated.identity => Some(Contaminated)
        case Prepared.identity => Some(Prepared)
        case _ => None
      }
    }

    @throws(classOf[IllegalArgumentException]) // scalastyle:ignore
    def unsafeFromInt(i: Int): SampleStatus = {
      fromInt(i).getOrElse {
        throw new IllegalArgumentException(s"Unknown sample status $i") // scalastyle:ignore
      }
    }

  }

  case object Ok extends SampleStatus {
    override val identity = 1
  }

  case object Destroyed extends SampleStatus {
    override val identity = 2
  }

  case object Contaminated extends SampleStatus {
    override val identity = 3
  }

  case object Prepared extends SampleStatus {
    override val identity = 4
  }

}
