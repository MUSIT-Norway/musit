package no.uio.musit.microservice.event.service

import no.uio.musit.microservice.event.dao.EventDao.EventBaseDto

/**
 * Created by ellenjo on 6/30/16.
 */

object EnvRequirementService extends SimpleService {
  def fromDatabase(id: Long, base: EventBaseDto) = ??? /*{
    MusitFuture.successful(Observation(Some(id), base.links, base.note, None))
  }*/

}