package models.analysis.events

import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models.ActorId
import org.joda.time.DateTime
import play.api.libs.json._

object AnalysisResults {

  trait ResultTypeCompanion extends WithDateTimeFormatters {
    val resultType: String
  }

  /**
   * Represents the base Result type defining which fields should be present
   * in _all_ result types.
   */
  sealed trait AnalysisResult {
    val registeredBy: Option[ActorId]
    val registeredDate: Option[DateTime]
    //    val restriction: Option[Restriction]
    val extRef: Option[Seq[String]]
    val comment: Option[String]

    def withtRegisteredDate(d: Option[DateTime]): AnalysisResult = {
      this match {
        case gr: GenericResult => gr.copy(registeredDate = d)
        case dr: DatingResult => dr.copy(registeredDate = d)
      }
    }

    def withRegisteredBy(a: Option[ActorId]): AnalysisResult = {
      this match {
        case gr: GenericResult => gr.copy(registeredBy = a)
        case dr: DatingResult => dr.copy(registeredBy = a)
      }
    }
  }

  object AnalysisResult extends WithDateTimeFormatters {

    private[this] val tpe = "type"

    implicit val reads: Reads[AnalysisResult] = Reads { jsv =>
      (jsv \ tpe).validate[String].flatMap {
        case GenericResult.resultType => GenericResult.format.reads(jsv)
        case DatingResult.resultType => DatingResult.format.reads(jsv)
      }
    }

    implicit val writes: Writes[AnalysisResult] = Writes {
      case genRes: GenericResult =>
        GenericResult.format.writes(genRes).as[JsObject] ++
          Json.obj(tpe -> GenericResult.resultType)

      case dteRes: DatingResult =>
        DatingResult.format.writes(dteRes).as[JsObject] ++
          Json.obj(tpe -> DatingResult.resultType)

    }

  }

  /**
   * Most analysis' will have this form of result. It provides a set of the most
   * commonly used fields to register different forms of results.
   *
   * @param extRef A list of references to external systems.
   * @param comment A comment field that may contain a hand written result
   */
  case class GenericResult(
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    extRef: Option[Seq[String]],
    comment: Option[String]
  ) extends AnalysisResult

  object GenericResult extends ResultTypeCompanion {
    override val resultType = "GenericResult"

    val format: Format[GenericResult] = Json.format[GenericResult]
  }

  /**
   * Events in the Dating category, that require some form of age result, will
   * typically be of this result type. It contains the common field, as well as
   * the age field.
   *
   * @param extRef A list of references to external systems.
   * @param comment A comment field that may contain a hand written result
   * @param age The result containing the specific dating result.
   */
  case class DatingResult(
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    extRef: Option[Seq[String]],
    comment: Option[String],
    age: Option[String]
  ) extends AnalysisResult

  object DatingResult extends ResultTypeCompanion {
    override val resultType = "DatingResult"

    val format: Format[DatingResult] = Json.format[DatingResult]
  }

}
