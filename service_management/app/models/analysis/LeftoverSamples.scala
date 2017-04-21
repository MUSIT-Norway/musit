package models.analysis

import play.api.libs.json._

object LeftoverSamples {

  sealed trait LeftoverSample {
    val key: Int
  }

  object LeftoverSample {
    def fromInt(i: Int): Option[LeftoverSample] = i match {
      case NotSpecified.key => Some(NotSpecified)
      case NoLeftover.key   => Some(NoLeftover)
      case HasLeftover.key  => Some(HasLeftover)
      case _                => None
    }

    @throws(classOf[IllegalArgumentException])
    def unsafeFromInt(i: Int): LeftoverSample = {
      fromInt(i).getOrElse {
        throw new IllegalArgumentException(s"Unknown residual material $i")
      }
    }

    implicit val reads: Reads[LeftoverSample] = Reads { jsv =>
      jsv.validate[Int] match {
        case JsSuccess(NotSpecified.key, _) => JsSuccess(NotSpecified)
        case JsSuccess(NoLeftover.key, _)   => JsSuccess(NoLeftover)
        case JsSuccess(HasLeftover.key, _)  => JsSuccess(HasLeftover)
        case JsSuccess(bad, p)              => JsError(p, s"Unknown residual material code $bad")
        case err: JsError                   => err
      }
    }

    implicit val writes: Writes[LeftoverSample] = Writes(rm => JsNumber(rm.key))

  }

  case object NotSpecified extends LeftoverSample {
    override val key = 1
  }

  case object NoLeftover extends LeftoverSample {
    override val key = 2
  }

  case object HasLeftover extends LeftoverSample {
    override val key = 3
  }

}
