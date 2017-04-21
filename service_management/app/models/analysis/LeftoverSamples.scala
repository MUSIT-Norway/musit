package models.analysis

import play.api.libs.json._

object LeftoverSamples {

  sealed trait LeftoverSample {
    val identity: Int
  }

  object LeftoverSample {
    def fromInt(i: Int): Option[LeftoverSample] = i match {
      case NotSpecified.identity => Some(NotSpecified)
      case NoLeftover.identity   => Some(NoLeftover)
      case HasLeftover.identity  => Some(HasLeftover)
      case _                     => None
    }

    @throws(classOf[IllegalArgumentException])
    def unsafeFromInt(i: Int): LeftoverSample = {
      fromInt(i).getOrElse {
        throw new IllegalArgumentException(s"Unknown residual material $i")
      }
    }

    implicit val reads: Reads[LeftoverSample] = Reads { jsv =>
      jsv.validate[Int] match {
        case JsSuccess(NotSpecified.identity, _) => JsSuccess(NotSpecified)
        case JsSuccess(NoLeftover.identity, _)   => JsSuccess(NoLeftover)
        case JsSuccess(HasLeftover.identity, _)  => JsSuccess(HasLeftover)
        case JsSuccess(bad, p)                   => JsError(p, s"Unknown residual material code $bad")
        case err: JsError                        => err
      }
    }

    implicit val writes: Writes[LeftoverSample] = Writes(rm => JsNumber(rm.identity))

  }

  case object NotSpecified extends LeftoverSample {
    override val identity = 1
  }

  case object NoLeftover extends LeftoverSample {
    override val identity = 2
  }

  case object HasLeftover extends LeftoverSample {
    override val identity = 3
  }

}
