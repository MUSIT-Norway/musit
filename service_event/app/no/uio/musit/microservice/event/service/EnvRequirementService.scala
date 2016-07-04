package no.uio.musit.microservice.event.service

import no.uio.musit.microservice.event.dao.EnvRequirementDAO
import no.uio.musit.microservice.event.dao.EnvRequirementDAO.EnvRequirementDto
import no.uio.musit.microservice.event.dao.EventDao.BaseEventDto
import no.uio.musit.microservice.event.domain.{EnvRequirement, _}
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ErrorHelper
import play.api.libs.json.{JsObject, JsResult, JsValue, Json}

/**
  * Created by ellenjo on 6/30/16.
  */

object EnvRequirementComplexEventType extends ComplexEventType {
  def fromDatabase(id: Long, baseEventDto: BaseEventDto) = {
    EnvRequirementDAO.getEnvRequirement(id)
      .toMusitFuture(ErrorHelper.badRequest(s"Unable to find observation with id: $id"))
      .musitFutureMap(envReq => new EnvRequirement(baseEventDto.props, envReq))
  }

  def createDatabaseInsertAction(id: Long, event: Event) = {
    val envReq = event.asInstanceOf[EnvRequirement]
    EnvRequirementDAO.insertAction(envReq.envReqDto.copy(id = Some(id)))
  }
}

object EnvRequirementJson extends JsonHandler {
  def fromJson(eventType: EventType, baseResult: JsResult[BaseEventProps], jsObject: JsObject): JsResult[EnvRequirement] = {
    for {
      baseProps <- baseResult
      envReqDto <- jsObject.validate[EnvRequirementDto]
    } yield new EnvRequirement(baseProps, envReqDto)
  }

  def toJson(event: Event): JsValue = Json.toJson(event.asInstanceOf[EnvRequirement].envReqDto)
}