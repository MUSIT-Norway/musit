package no.uio.musit.service

import play.api.mvc.BaseController

import scala.concurrent.ExecutionContext

trait MusitController extends BaseController with MusitActions {

  implicit def ec: ExecutionContext = defaultExecutionContext

}

trait MusitAdminController extends BaseController with MusitAdminActions {

  implicit def ec: ExecutionContext = defaultExecutionContext

}
