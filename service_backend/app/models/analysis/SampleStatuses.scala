package models.analysis

import play.api.libs.json._

object SampleStatuses {

  sealed trait SampleStatus {
    val key: Int
  }

  object SampleStatus {

    // scalastyle:off
    def fromInt(i: Int): Option[SampleStatus] = i match {
      case Intact.key       => Some(Intact)
      case Destroyed.key    => Some(Destroyed)
      case Contaminated.key => Some(Contaminated)
      case Prepared.key     => Some(Prepared)
      case Discarded.key    => Some(Discarded)
      case Cancelled.key    => Some(Cancelled)
      case Consumed.key     => Some(Consumed)
      case Dessicated.key   => Some(Dessicated)
      case Degraded.key     => Some(Degraded)
      case Mounted.key      => Some(Mounted)
      case _                => None
    }
    // scalastyle:on

    @throws(classOf[IllegalArgumentException])
    def unsafeFromInt(i: Int): SampleStatus = {
      fromInt(i).getOrElse {
        throw new IllegalArgumentException(s"Unknown sample status $i")
      }
    }

    implicit val reads: Reads[SampleStatus] = Reads { jsv =>
      jsv.validate[Int] match {
        case JsSuccess(Intact.key, _)       => JsSuccess(Intact)
        case JsSuccess(Destroyed.key, _)    => JsSuccess(Destroyed)
        case JsSuccess(Contaminated.key, _) => JsSuccess(Contaminated)
        case JsSuccess(Prepared.key, _)     => JsSuccess(Prepared)
        case JsSuccess(Discarded.key, _)    => JsSuccess(Discarded)
        case JsSuccess(Cancelled.key, _)    => JsSuccess(Cancelled)
        case JsSuccess(Consumed.key, _)     => JsSuccess(Consumed)
        case JsSuccess(Dessicated.key, _)   => JsSuccess(Dessicated)
        case JsSuccess(Degraded.key, _)     => JsSuccess(Degraded)
        case JsSuccess(Mounted.key, _)      => JsSuccess(Mounted)
        case JsSuccess(bad, p)              => JsError(p, s"Unknown sample status code $bad")
        case err: JsError                   => err
      }
    }

    implicit val writes: Writes[SampleStatus] = Writes(ss => JsNumber(ss.key))
  }

  case object Intact extends SampleStatus {
    override val key = 1
  }

  case object Destroyed extends SampleStatus {
    override val key = 2
  }

  case object Contaminated extends SampleStatus {
    override val key = 3
  }

  case object Prepared extends SampleStatus {
    override val key = 4
  }

  case object Discarded extends SampleStatus {
    override val key = 5
  }

  case object Cancelled extends SampleStatus {
    override val key = 6
  }

  case object Consumed extends SampleStatus {
    override val key = 7
  }

  case object Dessicated extends SampleStatus {
    override val key = 8
  }

  case object Degraded extends SampleStatus {
    override val key = 9
  }

  case object Mounted extends SampleStatus {
    override val key = 10
  }

}
