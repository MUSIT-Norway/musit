package no.uio.musit.service

import play.api.mvc.QueryStringBindable

class BindableOf[T](bindIt: Option[String] => Option[Either[String, T]]) extends QueryStringBindable[T] {
  def bind(key: String, data: Map[String, Seq[String]]): Option[Either[String, T]] = bindIt(data.get(key).flatMap(_.headOption))
  def unbind(key: String, mf: T): String = throw new NotImplementedError()
}
