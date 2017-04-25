package models.storage.event.observation

import models.storage.{FromToDouble, LifeCycle}
import no.uio.musit.formatters.StrictFormatters._
import play.api.libs.functional.syntax._
import play.api.libs.json._

object ObservationAttributes {

  // TODO: Document me!!!
  sealed trait ObservationAttribute {
    val note: Option[String]
  }

  // TODO: Document me!!!
  sealed trait ObservationFromTo extends ObservationAttribute {
    val range: FromToDouble
  }

  case class ObservationRelativeHumidity(
      note: Option[String],
      range: FromToDouble
  ) extends ObservationFromTo

  object ObservationRelativeHumidity {
    implicit val formats: Format[ObservationRelativeHumidity] = (
      (__ \ "note").formatNullable[String](maxCharsFormat(250)) and
        (__ \ "range").format[FromToDouble]
    )(ObservationRelativeHumidity.apply, unlift(ObservationRelativeHumidity.unapply))
  }

  case class ObservationTemperature(
      note: Option[String],
      range: FromToDouble
  ) extends ObservationFromTo

  object ObservationTemperature {
    implicit val formats: Format[ObservationTemperature] = (
      (__ \ "note").formatNullable[String](maxCharsFormat(250)) and
        (__ \ "range").format[FromToDouble]
    )(ObservationTemperature.apply, unlift(ObservationTemperature.unapply))
  }

  case class ObservationHypoxicAir(
      note: Option[String],
      range: FromToDouble
  ) extends ObservationFromTo

  object ObservationHypoxicAir {
    implicit val formats: Format[ObservationHypoxicAir] = (
      (__ \ "note").formatNullable[String](maxCharsFormat(250)) and
        (__ \ "range").format[FromToDouble]
    )(ObservationHypoxicAir.apply, unlift(ObservationHypoxicAir.unapply))
  }

  case class ObservationLightingCondition(
      note: Option[String],
      lightingCondition: Option[String]
  ) extends ObservationAttribute

  object ObservationLightingCondition {
    implicit val formats: Format[ObservationLightingCondition] = (
      (__ \ "note").formatNullable[String](maxCharsFormat(250)) and
        (__ \ "lightingCondition").formatNullable[String](maxCharsFormat(250))
    )(ObservationLightingCondition.apply, unlift(ObservationLightingCondition.unapply))
  }

  case class ObservationCleaning(
      note: Option[String],
      cleaning: Option[String]
  ) extends ObservationAttribute

  object ObservationCleaning {
    implicit val formats: Format[ObservationCleaning] = (
      (__ \ "note").formatNullable[String](maxCharsFormat(250)) and
        (__ \ "cleaning").formatNullable[String](maxCharsFormat(250))
    )(ObservationCleaning.apply, unlift(ObservationCleaning.unapply))
  }

  case class ObservationGas(
      note: Option[String],
      gas: Option[String]
  ) extends ObservationAttribute

  object ObservationGas {
    implicit val formats: Format[ObservationGas] = (
      (__ \ "note").formatNullable[String](maxCharsFormat(250)) and
        (__ \ "gas").formatNullable[String](maxCharsFormat(250))
    )(ObservationGas.apply, unlift(ObservationGas.unapply))
  }

  case class ObservationMold(
      note: Option[String],
      mold: Option[String]
  ) extends ObservationAttribute

  object ObservationMold {
    implicit val formats: Format[ObservationMold] = (
      (__ \ "note").formatNullable[String](maxCharsFormat(250)) and
        (__ \ "mold").formatNullable[String](maxCharsFormat(250))
    )(ObservationMold.apply, unlift(ObservationMold.unapply))
  }

  case class ObservationTheftProtection(
      note: Option[String],
      theftProtection: Option[String]
  ) extends ObservationAttribute

  object ObservationTheftProtection {
    implicit val formats: Format[ObservationTheftProtection] = (
      (__ \ "note").formatNullable[String](maxCharsFormat(250)) and
        (__ \ "theftProtection").formatNullable[String](maxCharsFormat(250))
    )(ObservationTheftProtection.apply, unlift(ObservationTheftProtection.unapply))
  }

  case class ObservationFireProtection(
      note: Option[String],
      fireProtection: Option[String]
  ) extends ObservationAttribute

  object ObservationFireProtection {
    implicit val formats: Format[ObservationFireProtection] = (
      (__ \ "note").formatNullable[String](maxCharsFormat(250)) and
        (__ \ "fireProtection").formatNullable[String](maxCharsFormat(250))
    )(ObservationFireProtection.apply, unlift(ObservationFireProtection.unapply))
  }

  case class ObservationPerimeterSecurity(
      note: Option[String],
      perimeterSecurity: Option[String]
  ) extends ObservationAttribute

  object ObservationPerimeterSecurity {
    implicit val formats: Format[ObservationPerimeterSecurity] = (
      (__ \ "note").formatNullable[String](maxCharsFormat(250)) and
        (__ \ "perimeterSecurity").formatNullable[String](maxCharsFormat(250))
    )(ObservationPerimeterSecurity.apply, unlift(ObservationPerimeterSecurity.unapply))
  }

  case class ObservationWaterDamageAssessment(
      note: Option[String],
      waterDamageAssessment: Option[String]
  ) extends ObservationAttribute

  object ObservationWaterDamageAssessment {
    implicit val formats: Format[ObservationWaterDamageAssessment] = (
      (__ \ "note").formatNullable[String](maxCharsFormat(250)) and
        (__ \ "waterDamageAssessment").formatNullable[String](maxCharsFormat(250))
    )(
      ObservationWaterDamageAssessment.apply,
      unlift(ObservationWaterDamageAssessment.unapply)
    ) // scalastyle:ignore
  }

  case class ObservationPest(
      note: Option[String],
      identification: Option[String],
      lifecycles: Seq[LifeCycle]
  ) extends ObservationAttribute

  object ObservationPest {
    implicit val formats: Format[ObservationPest] = (
      (__ \ "note").formatNullable[String](maxCharsFormat(250)) and
        (__ \ "identification").formatNullable[String](maxCharsFormat(250)) and
        (__ \ "lifecycles").format[Seq[LifeCycle]]
    )(ObservationPest.apply, unlift(ObservationPest.unapply))
  }

  case class ObservationAlcohol(
      note: Option[String],
      condition: Option[String],
      volume: Option[Double]
  ) extends ObservationAttribute

  object ObservationAlcohol {
    implicit val formats: Format[ObservationAlcohol] = (
      (__ \ "note").formatNullable[String](maxCharsFormat(250)) and
        (__ \ "condition").formatNullable[String](maxCharsFormat(250)) and
        (__ \ "volume").formatNullable[Double]
    )(ObservationAlcohol.apply, unlift(ObservationAlcohol.unapply))
  }

}
