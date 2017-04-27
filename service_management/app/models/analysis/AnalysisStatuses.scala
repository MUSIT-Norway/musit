package models.analysis

import play.api.libs.json._

object AnalysisStatuses {

  trait AnalysisStatus {
    val key: Int
  }

  object AnalysisStatus {
    def fromInt(i: Int): Option[AnalysisStatus] = {
      i match {
        case Preparation.key         => Some(Preparation)
        case AwaitingResult.key      => Some(AwaitingResult)
        case Finished.key            => Some(Finished)
        case ClosedWithoutResult.key => Some(ClosedWithoutResult)
        case _                       => None
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
        case JsSuccess(Preparation.key, _)         => JsSuccess(Preparation)
        case JsSuccess(AwaitingResult.key, _)      => JsSuccess(AwaitingResult)
        case JsSuccess(Finished.key, _)            => JsSuccess(Finished)
        case JsSuccess(ClosedWithoutResult.key, _) => JsSuccess(ClosedWithoutResult)
        case JsSuccess(bad, p)                     => JsError(p, s"Unknown sample status code $bad")
        case err: JsError                          => err
      }
    }

    implicit val writes: Writes[AnalysisStatus] = Writes(ss => JsNumber(ss.key))
  }

  object Preparation extends AnalysisStatus {
    val key = 1
  }

  object AwaitingResult extends AnalysisStatus {
    val key = 2
  }

  object Finished extends AnalysisStatus {
    val key = 3
  }

  object ClosedWithoutResult extends AnalysisStatus {
    val key = 4
  }

}
