package no.uio.musit.service

import play.api.mvc.QueryStringBindable

// $COVERAGE-OFF$
class BindableOf[T](
    bindIt: Option[String] => Option[Either[String, T]]
) extends QueryStringBindable[T] {

  override def bind(
      key: String,
      data: Map[String, Seq[String]]
  ): Option[Either[String, T]] = bindIt(data.get(key).flatMap(_.headOption))

  override def unbind(key: String, mf: T): String =
    throw new NotImplementedError()
}
// $COVERAGE-ON$
