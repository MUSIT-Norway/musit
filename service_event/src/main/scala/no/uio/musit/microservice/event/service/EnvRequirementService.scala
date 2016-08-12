package no.uio.musit.microservice.event.service

import com.google.inject.Inject
import no.uio.musit.microservice.event.dao.EnvRequirementDao
import no.uio.musit.microservice.event.domain._
import no.uio.musit.microservice.event.domain.dto.BaseEventDto
import no.uio.musit.microservice.event.dto.{BaseEventDto, Dto, EnvRequirementDto}
import play.api.libs.json.{JsObject, JsResult, Json}

import scala.concurrent.Future

case class EnvRequirement(baseProps: BaseEventDto, envReqDto: EnvRequirementDto) extends Event(baseProps) {
  val temperature = envReqDto.temperature
  val airHumidity = envReqDto.airHumidity
  //publish all the fields or none...

}

class EnvRequirementService @Inject()(val envRequirementDao: EnvRequirementDao) extends MultipleTablesNotUsingCustomFields {

  def createEventInMemory(baseProps: BaseEventDto, customDto: Dto): Event = EnvRequirement(baseProps, customDto.asInstanceOf[EnvRequirementDto])

  def getCustomDtoFromDatabase(id: Long, baseEventProps: BaseEventDto): Future[Option[Dto]] = envRequirementDao.getEnvRequirement(id)

  def createInsertCustomDtoAction(id: Long, event: Event) = {
    val specificEvent = event.asInstanceOf[EnvRequirement]
    envRequirementDao.insertAction(specificEvent.envReqDto.copy(id = Some(id)))
  }

  def validateCustomDto(jsObject: JsObject): JsResult[Dto] = jsObject.validate[EnvRequirementDto]

  def customDtoToJson(event: Event): JsObject = Json.toJson(event.asInstanceOf[EnvRequirement].envReqDto).asInstanceOf[JsObject]

}
