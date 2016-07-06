package no.uio.musit.microservice.event.service

import no.uio.musit.microservice.event.dao.EnvRequirementDAO
import no.uio.musit.microservice.event.dao.EnvRequirementDAO.EnvRequirementDto
import no.uio.musit.microservice.event.dao.EventDao.BaseEventDto
import no.uio.musit.microservice.event.domain._
import play.api.libs.json.{ JsObject, JsResult, Json }

import scala.concurrent.Future

class EnvRequirement(val baseProps: BaseEventProps, val envReqDto: EnvRequirementDto) extends Event(baseProps) {
  val temperature = envReqDto.temperature
  val airHumidity = envReqDto.airHumidity
  //publish all the fields or none...

}

object EnvRequirementService extends MultipleTablesMultipleDtos {

  def createEventInMemory(baseProps: BaseEventProps, customDto: Dto): Event = new EnvRequirement(baseProps, customDto.asInstanceOf[EnvRequirementDto])

  def getCustomDtoFromDatabase(id: Long, baseEventProps: BaseEventProps): Future[Option[Dto]] = EnvRequirementDAO.getEnvRequirement(id)

  def createInsertCustomDtoAction(id: Long, event: Event) = {
    val specificEvent = event.asInstanceOf[EnvRequirement]
    EnvRequirementDAO.insertAction(specificEvent.envReqDto.copy(id = Some(id)))
  }

  def validateCustomDto(jsObject: JsObject): JsResult[Dto] = jsObject.validate[EnvRequirementDto]

  def customDtoToJson(event: Event): JsObject = Json.toJson(event.asInstanceOf[EnvRequirement].envReqDto).asInstanceOf[JsObject]
}
