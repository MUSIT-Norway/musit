package no.uio.musit.musitUtils

import no.uio.musit.MusitResults.{MusitInternalError, MusitResult, MusitSuccess}

import scala.concurrent.{ExecutionContext, Future}

object Utils {

  def musitResultToOption[T](
      value: MusitResult[Option[T]]
  ): Option[T] = {
    value match {
      case MusitSuccess(t) => t
      case err             => None
    }
  }

  /** Gitt en liste med Id-er av en eller annen type, og en funksjon som tar en slik Id og gir tilbake en
   * Future[MusitResult[Option[T]]] (typisk et database-kall ala findById, getById etc), returner en liste med id og evt svar
   * @param seq
   * @param f
   * @tparam Id
   * @tparam T
   * @return
   */
  def getIdAndObjectList[Id, A](
      seq: Seq[Id],
      f: Id => Future[MusitResult[Option[A]]]
  )(implicit ec: ExecutionContext): Future[Seq[(Id, Option[A])]] = {

    def fMerket(id: Id): Future[(Id, MusitResult[Option[A]])] = {
      f(id).map(mrResult => (id, mrResult))
    }
    val seqFuts = seq.map(id => fMerket(id))
    val futSeq  = Future.sequence(seqFuts)
    futSeq.map(
      seq => seq.map { case (id, mrOpt) => (id, musitResultToOption(mrOpt)) }
    )

  }

  /** Checks whether we have any (Id, None) pairs in the sequence and if so collects these "buggy" ids and returns a fail,
   * else we return the sequence of Ts (everything inside a Future[MusitResult])
   *
   *
   * Typically useful with getBlabla.
   * */
  def makeIntoMusitResult[Id, A](
      futSeq: Future[Seq[(Id, Option[A])]],
      errorMsg: Seq[Id] => String
  )(implicit ec: ExecutionContext): Future[MusitResult[Seq[A]]] = {
    futSeq.map { seq =>
      val idsWithoutEvents = seq.collect { case (id, None) => id }
      if (idsWithoutEvents.isEmpty) {
        MusitSuccess(seq.collect { case (id, Some(event)) => event })
      } else MusitInternalError(errorMsg(idsWithoutEvents))
    }
  }

  def mapIdsToObjects[Id, A](
      ids: Seq[Id],
      f: Id => Future[MusitResult[Option[A]]],
      errorMsg: String
  )(implicit ec: ExecutionContext): Future[MusitResult[Seq[A]]] = {
    makeIntoMusitResult[Id, A](
      getIdAndObjectList(ids, f),
      idsWithoutEvents => s"$errorMsg + $idsWithoutEvents"
    )
  }

}
