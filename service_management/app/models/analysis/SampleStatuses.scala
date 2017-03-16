package models.analysis

import play.api.libs.json._

object SampleStatuses {

  sealed trait SampleStatus {
    val identity: Int
  }

  object SampleStatus {

    def fromInt(i: Int): Option[SampleStatus] = {
      i match {
        case Intact.identity => Some(Intact)
        case Destroyed.identity => Some(Destroyed)
        case Contaminated.identity => Some(Contaminated)
        case Prepared.identity => Some(Prepared)
        case _ => None
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
        case JsSuccess(Intact.identity, _) => JsSuccess(Intact)
        case JsSuccess(Destroyed.identity, _) => JsSuccess(Destroyed)
        case JsSuccess(Contaminated.identity, _) => JsSuccess(Contaminated)
        case JsSuccess(Prepared.identity, _) => JsSuccess(Prepared)
        case JsSuccess(bad, p) => JsError(p, s"Unknown sample status code $bad")
        case err: JsError => err
      }
    }

    implicit val writes: Writes[SampleStatus] = Writes(ss => JsNumber(ss.identity))
  }

  case object Intact extends SampleStatus {
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
