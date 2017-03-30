package services.analysis

import com.google.inject.Inject
import models.analysis.SampleObject
import models.analysis.events.SampleCreated
import no.uio.musit.MusitResults._
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models.{MuseumId, ObjectUUID}
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time
import no.uio.musit.time.dateTimeNow
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.analysis.dao.SampleObjectDao

import scala.concurrent.Future

class SampleObjectService @Inject()(
    val soDao: SampleObjectDao
) {

  val logger = Logger(classOf[SampleObjectService])

  def add(
      so: SampleObject
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[ObjectUUID]] = {
    val sobj = so.copy(
      objectId = ObjectUUID.generateAsOpt(),
      registeredBy = Some(currUser.id),
      registeredDate = Some(dateTimeNow)
    )

    if (so.isExtracted) {
      val eventObj = SampleCreated(
        id = None,
        eventDate = sobj.registeredDate,
        registeredBy = sobj.registeredBy,
        registeredDate = sobj.registeredDate,
        objectId = sobj.parentObjectId,
        sampleObjectId = sobj.objectId
      )
      soDao.insert(sobj, eventObj)
    } else {
      soDao.insert(sobj)
    }
  }

  def update(
      oid: ObjectUUID,
      so: SampleObject
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[Option[SampleObject]]] = {
    def enrich(orig: SampleObject) = {
      so.copy(
        objectId = Some(oid),
        registeredBy = orig.registeredBy,
        registeredDate = orig.registeredDate,
        updatedBy = Some(currUser.id),
        updatedDate = Some(dateTimeNow)
      )
    }

    val updatedRes = for {
      orig <- MusitResultT(findById(oid, MusitEmpty))
      _    <- MusitResultT(soDao.update(enrich(orig)))
      upd <- MusitResultT(
              findById(
                oid,
                MusitInternalError(s"Couldn't find sample $oid after update")
              )
            )
    } yield upd

    // Need to do some tricks to align the shapes again.
    updatedRes.value.map {
      case MusitSuccess(updatedObj) => MusitSuccess(Option(updatedObj))
      case MusitEmpty               => MusitSuccess(None)
      case err: MusitError          => err
    }
  }

  private def findById(
      oid: ObjectUUID,
      notFound: MusitError
  ): Future[MusitResult[SampleObject]] = {
    findById(oid).map(_.flatMap(_.map(MusitSuccess.apply).getOrElse(notFound)))
  }

  def findById(oid: ObjectUUID): Future[MusitResult[Option[SampleObject]]] = {
    soDao.findByUUID(oid)
  }

  def findForParent(oid: ObjectUUID): Future[MusitResult[Seq[SampleObject]]] = {
    soDao.listForParentObject(oid)
  }

  def findForMuseum(mid: MuseumId): Future[MusitResult[Seq[SampleObject]]] = {
    soDao.listForMuseum(mid)
  }

}
