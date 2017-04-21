package models.analysis

import play.api.libs.json._

object SampleStatuses {

  sealed trait SampleStatus {
    val key: Int
  }

  object SampleStatus {

    def fromInt(i: Int): Option[SampleStatus] = {
      i match {
        case Intact.key       => Some(Intact)
        case Destroyed.key    => Some(Destroyed)
        case Contaminated.key => Some(Contaminated)
        case Prepared.key     => Some(Prepared)
        case _                => None
      }
    }

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

}
