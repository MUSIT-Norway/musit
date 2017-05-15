package models.analysis.events

import models.analysis.Size
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
    val extRef: Option[Seq[String]]
    val comment: Option[String]

    def withtRegisteredDate(d: Option[DateTime]): AnalysisResult = {
      this match {
        case gr: GenericResult     => gr.copy(registeredDate = d)
        case dr: AgeResult         => dr.copy(registeredDate = d)
        case rc: RadioCarbonResult => rc.copy(registeredDate = d)
        case mr: MeasurementResult => mr.copy(registeredDate = d)
        case er: ExtractionResult  => er.copy(registeredDate = d)
      }
    }

    def withRegisteredBy(a: Option[ActorId]): AnalysisResult = {
      this match {
        case gr: GenericResult     => gr.copy(registeredBy = a)
        case dr: AgeResult         => dr.copy(registeredBy = a)
        case rc: RadioCarbonResult => rc.copy(registeredBy = a)
        case mr: MeasurementResult => mr.copy(registeredBy = a)
        case er: ExtractionResult  => er.copy(registeredBy = a)
      }
    }
  }

  object AnalysisResult extends WithDateTimeFormatters {

    private[this] val tpe = "type"

    implicit val reads: Reads[AnalysisResult] = Reads { jsv =>
      (jsv \ tpe).validate[String].flatMap {
        case GenericResult.resultType     => GenericResult.format.reads(jsv)
        case AgeResult.resultType         => AgeResult.format.reads(jsv)
        case RadioCarbonResult.resultType => RadioCarbonResult.format.reads(jsv)
        case MeasurementResult.resultType => MeasurementResult.format.reads(jsv)
        case ExtractionResult.resultType  => ExtractionResult.format.reads(jsv)
      }
    }

    implicit val writes: Writes[AnalysisResult] = Writes {
      case genRes: GenericResult =>
        GenericResult.format.writes(genRes).as[JsObject] ++
          Json.obj(tpe -> GenericResult.resultType)

      case ageRes: AgeResult =>
        AgeResult.format.writes(ageRes).as[JsObject] ++
          Json.obj(tpe -> AgeResult.resultType)

      case racRes: RadioCarbonResult =>
        RadioCarbonResult.format.writes(racRes).as[JsObject] ++
          Json.obj(tpe -> RadioCarbonResult.resultType)

      case mesRes: MeasurementResult =>
        MeasurementResult.format.writes(mesRes).as[JsObject] ++
          Json.obj(tpe -> MeasurementResult.resultType)

      case extRes: ExtractionResult =>
        ExtractionResult.format.writes(extRes).as[JsObject] ++
          Json.obj(tpe -> ExtractionResult.resultType)

    }

  }

  /**
   * Most analysis' will have this form of result. It provides a set of the most
   * commonly used fields to register different forms of results.
   *
   * @param registeredBy The ActorId that registered the result
   * @param registeredDate The date when the result was registered
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
   * @param registeredBy The ActorId that registered the result
   * @param registeredDate The date when the result was registered
   * @param extRef A list of references to external systems.
   * @param comment A comment field that may contain a hand written result
   * @param age The result containing the specific dating result.
   */
  case class AgeResult(
      registeredBy: Option[ActorId],
      registeredDate: Option[DateTime],
      extRef: Option[Seq[String]],
      comment: Option[String],
      age: Option[String]
  ) extends AnalysisResult

  object AgeResult extends ResultTypeCompanion {
    override val resultType = "AgeResult"

    val format: Format[AgeResult] = Json.format[AgeResult]
  }

  /**
   * AnalysisResult type specifically to related to carbon dating analysis.
   *
   * @param registeredBy The ActorId that registered the result
   * @param registeredDate The date when the result was registered
   * @param extRef A list of references to external systems.
   * @param comment A comment field that may contain a hand written result
   * @param ageEstimate The estimated radio carbon date
   * @param standardDeviation The estimated standard deviation
   */
  case class RadioCarbonResult(
      registeredBy: Option[ActorId],
      registeredDate: Option[DateTime],
      extRef: Option[Seq[String]],
      comment: Option[String],
      ageEstimate: Option[String],
      standardDeviation: Option[String]
  ) extends AnalysisResult

  object RadioCarbonResult extends ResultTypeCompanion {
    override val resultType = "RadioCarbonResult"

    val format: Format[RadioCarbonResult] = Json.format[RadioCarbonResult]
  }

  /**
   * AnalysisResult type for registering measurements and counts.
   *
   * @param registeredBy The ActorId that registered the result
   * @param registeredDate The date when the result was registered
   * @param extRef A list of references to external systems.
   * @param comment A comment field that may contain a hand written result
   * @param measurementType The type of measurement performed.
   * @param size The measured size.
   * @param precision The precision of the measured size.
   * @param method The method that was used to perform the measurement.
   */
  case class MeasurementResult(
      registeredBy: Option[ActorId],
      registeredDate: Option[DateTime],
      extRef: Option[Seq[String]],
      comment: Option[String],
//      id: String, // TODO: What is this, and what is it supposed to be used for?
      measurementType: Option[String],
      size: Option[Size],
      precision: Option[String],
      method: Option[String]
  ) extends AnalysisResult

  object MeasurementResult extends ResultTypeCompanion {
    override val resultType = "MeasurementResult"

    implicit val format: Format[MeasurementResult] = Json.format[MeasurementResult]
  }

  /**
   * AnalysisResult type for registering results for a genetic extraction.
   *
   * @param registeredBy The ActorId that registered the result
   * @param registeredDate The date when the result was registered
   * @param extRef A list of references to external systems.
   * @param comment A comment field that may contain a hand written result
   * @param storageMedium The storage medium used to keep the extracted result in.
   * @param concentration The concentration of the extracted material.
   * @param volume The size / volume of the extracted material
   */
  case class ExtractionResult(
      registeredBy: Option[ActorId],
      registeredDate: Option[DateTime],
      extRef: Option[Seq[String]],
      comment: Option[String],
      storageMedium: Option[String],
      concentration: Option[Size],
      volume: Option[Size]
  ) extends AnalysisResult

  object ExtractionResult extends ResultTypeCompanion {
    override val resultType = "ExtractionResult"

    implicit val format: Format[ExtractionResult] = Json.format[ExtractionResult]
  }
}
