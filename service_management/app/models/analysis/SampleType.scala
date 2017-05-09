package models.analysis

import play.api.libs.json.Json

case class SampleType(
    sampleTypeId: SampleTypeId,
    noSampleType: String,
    enSampleType: String,
    noSampleSubType: Option[String],
    enSampleSubType: Option[String]
)

object SampleType {

  implicit val format = Json.format[SampleType]

}
