package services.analysis

import com.google.inject.Inject
import models.analysis.{ActorStamp, SampleObject}
import models.analysis.events.SampleCreated
import no.uio.musit.MusitResults._
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models.{MuseumId, ObjectUUID}
import no.uio.musit.security.AuthenticatedUser
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
      registeredStamp = Some(ActorStamp(currUser.id, dateTimeNow))
    )

    if (so.isExtracted) {
      val eventObj = SampleCreated(
        id = None,
        doneBy = sobj.registeredStamp.map(_.user),
        doneDate = sobj.registeredStamp.map(_.date),
        registeredBy = sobj.registeredStamp.map(_.user),
        registeredDate = sobj.registeredStamp.map(_.date),
        objectId = sobj.parentObjectId,
        sampleObjectId = sobj.objectId,
        externalLinks = None
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
        registeredStamp = orig.registeredStamp,
        updatedStamp = Some(ActorStamp(currUser.id, dateTimeNow)),
        isDeleted = orig.isDeleted
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
    updatedRes.value.map(_.map(Option.apply))
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

  def delete(
      oid: ObjectUUID
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[Unit]] = {
    def enrich(orig: SampleObject) = {
      orig.copy(
        isDeleted = true,
        updatedStamp = Some(ActorStamp(currUser.id, dateTimeNow))
      )
    }

    val updatedRes = for {
      orig <- MusitResultT(findById(oid, MusitEmpty))
      del  <- MusitResultT(soDao.update(enrich(orig))).map(_ => MusitSuccess(()))
    } yield del

    // Need to do some tricks to align the shapes again.
    updatedRes.value.map(_ => MusitSuccess(()))
  }

}
