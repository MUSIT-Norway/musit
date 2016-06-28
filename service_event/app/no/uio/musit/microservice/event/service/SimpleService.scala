package no.uio.musit.microservice.event.service

abstract class SimpleService extends EventService {
  def maybeActionCreator = None
}
