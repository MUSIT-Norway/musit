package no.uio.musit.microservice.storagefacility.domain.report

import play.api.libs.json.{Format, Json}

/**
  * Created by ellenjo on 26.09.16.
  */
  case class KdReport(totalArea:Int,
                    perimeterSecurity: Int,
                    theftProtection: Int,
                    fireProtection: Int,
                    waterDamageAssessment : Int,
                    routinesAndContingencyPlan: Int)

object KdReport {

  implicit val format: Format[KdReport] =
    Json.format[KdReport]}

