package no.uio.musit.service

import play.api.mvc.Controller

trait MusitController extends Controller with MusitActions

trait MusitAdminController extends Controller with MusitAdminActions
