package models.analysis

import play.api.libs.json._

object AnalysisStatuses {

  trait AnalysisStatus {
    val key: Int
  }

  object AnalysisStatus {
    def fromInt(i: Int): Option[AnalysisStatus] = {
      i match {
        case Prepared.key          => Some(Prepared)
        case AwaitingResult.key    => Some(AwaitingResult)
        case Done.key              => Some(Done)
        case DoneWithoutResult.key => Some(DoneWithoutResult)
        case _                     => None
      }
    }

    @throws(classOf[IllegalArgumentException])
    def unsafeFromInt(i: Int): AnalysisStatus = {
      fromInt(i).getOrElse {
        throw new IllegalArgumentException(s"Unknown sample status $i")
      }
    }

    implicit val reads: Reads[AnalysisStatus] = Reads { jsv =>
      jsv.validate[Int] match {
        case JsSuccess(Prepared.key, _)          => JsSuccess(Prepared)
        case JsSuccess(AwaitingResult.key, _)    => JsSuccess(AwaitingResult)
        case JsSuccess(Done.key, _)              => JsSuccess(Done)
        case JsSuccess(DoneWithoutResult.key, _) => JsSuccess(DoneWithoutResult)

        case JsSuccess(bad, p) => JsError(p, s"Unknown sample status code $bad")
        case err: JsError      => err
      }
    }

    implicit val writes: Writes[AnalysisStatus] = Writes(ss => JsNumber(ss.key))
  }

  object Prepared extends AnalysisStatus {
    val key = 1
  }

  object AwaitingResult extends AnalysisStatus {
    val key = 2
  }

  object Done extends AnalysisStatus {
    val key = 3
  }

  object DoneWithoutResult extends AnalysisStatus {
    val key = 4
  }

}
