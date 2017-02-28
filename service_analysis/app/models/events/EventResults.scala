package models.events

import play.api.libs.json._

object EventResults {

  trait ResultTypeCompanion {
    val resultTypeId: Int
  }

  sealed trait Result {
    val extRef: Option[Seq[String]]
    val comment: Option[String]
  }

  object Result {

    private[this] val tpe = "resType"

    implicit val reads: Reads[Result] = Reads { jsv =>
      (jsv \ tpe).validate[Int].flatMap {
        case GeneralResult.resultTypeId => GeneralResult.format.reads(jsv)
        case DatingResult.resultTypeId => DatingResult.format.reads(jsv)
      }
    }

    implicit val writes: Writes[Result] = Writes {
      case genRes: GeneralResult =>
        GeneralResult.format.writes(genRes).as[JsObject] ++
          Json.obj(tpe -> GeneralResult.resultTypeId)

      case dteRes: DatingResult =>
        DatingResult.format.writes(dteRes).as[JsObject] ++
          Json.obj(tpe -> DatingResult.resultTypeId)

    }

  }

  case class GeneralResult(
    extRef: Option[Seq[String]],
    comment: Option[String]
  ) extends Result

  object GeneralResult extends ResultTypeCompanion {
    override val resultTypeId = 1

    val format: Format[GeneralResult] = Json.format[GeneralResult]
  }

  case class DatingResult(
    extRef: Option[Seq[String]],
    comment: Option[String],
    age: Option[String]
  ) extends Result

  object DatingResult extends ResultTypeCompanion {
    override val resultTypeId = 2

    val format: Format[DatingResult] = Json.format[DatingResult]
  }

}
