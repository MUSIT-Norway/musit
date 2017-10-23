package no.uio.musit.models

case class MusitSlickClientError(msg: String) extends Exception(msg) {}
