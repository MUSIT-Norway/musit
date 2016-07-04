package no.uio.musit.microservice.event.service

// import no.uio.musit.microservice.event.domain.{ Control, ControlTemperature }
import no.uio.musit.microservices.common.extensions.FutureExtensions._

// TODO:
/*
object ControlService extends SimpleService {

  override def fromDatabase(id: Long, base: EventBaseDto) =
    MusitFuture.successful(Control(Some(id), base.links, base.note, None))

}

object ControlTemperatureService extends SimpleService {

  override def fromDatabase(id: Long, base: EventBaseDto) =
    MusitFuture.successful(ControlTemperature(Some(id), base.links, base.note, None, base.valueLongToBool, None))

}
*/