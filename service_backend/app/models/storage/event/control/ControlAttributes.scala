package models.storage.event.control

import models.storage.event.observation.ObservationAttributes._
import play.api.libs.json._

object ControlAttributes {

  sealed trait ControlAttribute {
    val ok: Boolean
    val observation: Option[ObservationAttribute]
  }

  /**
   * Since all ControlSubEvents have the same general structure, we can use
   * a generic function to implement a JSON formatter for these.
   *
   * @param apply function to create a new instance of A. Typically the apply
   *              method that belongs to the specific implementation.
   * @tparam A the type of ControlSubEvent to process
   * @tparam B the type of ObservationSubEvent that belongs to A
   * @return a play.api.libs.json.Format[A]
   */
  private def jsFormat[A <: ControlAttribute, B <: ObservationAttribute](
      apply: (Boolean, Option[B]) => A
  )(implicit obsFormat: Format[B]): Format[A] = Format(
    fjs = Reads[A] { jsv =>
      val ok       = (jsv \ "ok").as[Boolean]
      val maybeObs = (jsv \ "observation").asOpt[B]

      if (ok && maybeObs.isDefined) {
        JsError("A control that is OK cannot also have an observation")
      } else if (!ok && maybeObs.isEmpty) {
        JsError("A control that is not OK must have an observation")
      } else {
        JsSuccess(apply(ok, maybeObs))
      }
    },
    tjs = Writes[A] { cs =>
      Json.obj("ok" -> cs.ok) ++ cs.observation.map { obs =>
        Json.obj("observation" -> obsFormat.writes(obs.asInstanceOf[B]))
      }.getOrElse(Json.obj())
    }
  )

  case class ControlAlcohol(
      ok: Boolean,
      observation: Option[ObservationAlcohol]
  ) extends ControlAttribute

  object ControlAlcohol {
    implicit val formats: Format[ControlAlcohol] =
      jsFormat[ControlAlcohol, ObservationAlcohol](ControlAlcohol.apply)
  }

  case class ControlCleaning(
      ok: Boolean,
      observation: Option[ObservationCleaning]
  ) extends ControlAttribute

  object ControlCleaning {
    implicit val formats: Format[ControlCleaning] =
      jsFormat[ControlCleaning, ObservationCleaning](ControlCleaning.apply)
  }

  case class ControlGas(
      ok: Boolean,
      observation: Option[ObservationGas]
  ) extends ControlAttribute

  object ControlGas {
    implicit val formats: Format[ControlGas] =
      jsFormat[ControlGas, ObservationGas](ControlGas.apply)
  }

  case class ControlHypoxicAir(
      ok: Boolean,
      observation: Option[ObservationHypoxicAir]
  ) extends ControlAttribute

  object ControlHypoxicAir {
    implicit val formats: Format[ControlHypoxicAir] =
      jsFormat[ControlHypoxicAir, ObservationHypoxicAir](ControlHypoxicAir.apply)
  }

  case class ControlLightingCondition(
      ok: Boolean,
      observation: Option[ObservationLightingCondition]
  ) extends ControlAttribute

  object ControlLightingCondition {
    implicit val formats: Format[ControlLightingCondition] =
      jsFormat[ControlLightingCondition, ObservationLightingCondition](
        ControlLightingCondition.apply
      )
  }

  case class ControlMold(
      ok: Boolean,
      observation: Option[ObservationMold]
  ) extends ControlAttribute

  object ControlMold {
    implicit val formats: Format[ControlMold] =
      jsFormat[ControlMold, ObservationMold](ControlMold.apply)
  }

  case class ControlPest(
      ok: Boolean,
      observation: Option[ObservationPest]
  ) extends ControlAttribute

  object ControlPest {
    implicit val formats: Format[ControlPest] =
      jsFormat[ControlPest, ObservationPest](ControlPest.apply)
  }

  case class ControlRelativeHumidity(
      ok: Boolean,
      observation: Option[ObservationRelativeHumidity]
  ) extends ControlAttribute

  object ControlRelativeHumidity {
    implicit val formats: Format[ControlRelativeHumidity] =
      jsFormat[ControlRelativeHumidity, ObservationRelativeHumidity](
        ControlRelativeHumidity.apply
      )
  }

  case class ControlTemperature(
      ok: Boolean,
      observation: Option[ObservationTemperature]
  ) extends ControlAttribute

  object ControlTemperature {
    implicit val formats: Format[ControlTemperature] =
      jsFormat[ControlTemperature, ObservationTemperature](
        ControlTemperature.apply
      )
  }

}
