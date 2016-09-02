package no.uio.musit.microservice.event.service

import no.uio.musit.microservice.event.dao.EnvRequirementDao
import no.uio.musit.microservice.event.domain._
import play.api.libs.json.{ JsObject, JsResult, Json }
import slick.dbio.DBIO

import scala.concurrent.Future

class EnvRequirement(val baseProps: BaseEventDto, val envReqDto: EnvRequirementDto) extends Event(baseProps) {
  val temperature = envReqDto.temperature
  val airHumidity = envReqDto.airHumidity

  def doExecute(eventId: Long): DBIO[Unit] = {
    EnvRequirementDao.execute(eventId, this)
  }
  override def execute = Some(doExecute)
  //publish all the fields or none...

}

object EnvRequirementService extends MultipleTablesNotUsingCustomFields {

  override def storeObjectsInPlaceRelationTable = true // place is viewed as object in this event.

  def createEventInMemory(baseProps: BaseEventDto, customDto: Dto): Event = new EnvRequirement(baseProps, customDto.asInstanceOf[EnvRequirementDto])

  def getCustomDtoFromDatabase(id: Long, baseEventProps: BaseEventDto): Future[Option[Dto]] = EnvRequirementDao.getEnvRequirement(id)

  def createInsertCustomDtoAction(id: Long, event: Event) = {
    val specificEvent = event.asInstanceOf[EnvRequirement]
    EnvRequirementDao.insertAction(specificEvent.envReqDto.copy(id = Some(id)))
  }

  def validateCustomDto(jsObject: JsObject): JsResult[Dto] = jsObject.validate[EnvRequirementDto]

  def customDtoToJson(event: Event): JsObject = Json.toJson(event.asInstanceOf[EnvRequirement].envReqDto).asInstanceOf[JsObject]
}
