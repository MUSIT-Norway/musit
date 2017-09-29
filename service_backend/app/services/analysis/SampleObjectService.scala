package services.analysis

import com.google.inject.Inject
import models.analysis.events.SampleCreated
import models.analysis.{ActorStamp, EnrichedSampleObject, SampleObject}
import no.uio.musit.MusitResults._
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models._
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import play.api.Logger
import repositories.analysis.dao.SampleObjectDao
import services.musitobject.ObjectService

import scala.concurrent.{ExecutionContext, Future}

class SampleObjectService @Inject()(
    implicit
    val soDao: SampleObjectDao,
    val objService: ObjectService,
    val ec: ExecutionContext
) {

  val logger = Logger(classOf[SampleObjectService])

  def add(
      mid: MuseumId,
      so: SampleObject
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[ObjectUUID]] = {
    objService
      .findByUUID(mid, so.originatedObjectUuid, currUser.collectionsFor(mid))
      .flatMap {
        case MusitSuccess(maybeObject) =>
          maybeObject.map { origObj =>
            val sobj = so.copy(
              objectId = ObjectUUID.generateAsOpt(),
              registeredStamp = Some(ActorStamp(currUser.id, dateTimeNow))
            )

            if (so.isExtracted) {
              val eventObj = SampleCreated(
                id = None,
                doneBy = sobj.doneByStamp.map(_.user),
                doneDate = sobj.doneByStamp.map(_.date),
                registeredBy = sobj.registeredStamp.map(_.user),
                registeredDate = sobj.registeredStamp.map(_.date),
                affectedThing = sobj.parentObject.objectId,
                sampleObjectId = sobj.objectId,
                externalLinks = None
              )
              soDao.insert(mid, sobj, eventObj)
            } else {
              soDao.insert(sobj)
            }
          }.getOrElse {
            Future.successful {
              MusitValidationError(
                s"Trying to add a SampleObject with an originating object UUID " +
                  s"[${so.originatedObjectUuid}] that is not referring to a " +
                  s"collection object."
              )
            }
          }

        case err: MusitError =>
          Future.successful(err)
      }

  }

  def update(
      oid: ObjectUUID,
      so: SampleObject
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[Option[SampleObject]]] = {
    def enrich(orig: SampleObject) = {
      so.copy(
        objectId = Some(oid),
        sampleNum = orig.sampleNum,
        registeredStamp = orig.registeredStamp,
        updatedStamp = Some(ActorStamp(currUser.id, dateTimeNow)),
        isDeleted = orig.isDeleted
      )
    }

    val updatedRes = for {
      orig <- MusitResultT(findById(oid, MusitNotFound(s"Couldn't find sample $oid")))
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
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[SampleObject]] = {
    findById(oid).map(_.flatMap(_.map(MusitSuccess.apply).getOrElse(notFound)))
  }

  def findById(
      oid: ObjectUUID
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[SampleObject]]] = {
    soDao.findByUUID(oid)
  }

  def findForParent(
      oid: ObjectUUID
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Seq[SampleObject]]] = {
    soDao.listForParentObject(oid)
  }

  def findForOriginating(
      oid: ObjectUUID
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Seq[SampleObject]]] = {
    soDao.listForOriginatingObject(oid)
  }

  def findForMuseum(
      mid: MuseumId
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Seq[SampleObject]]] = {
    soDao.listForMuseum(mid)
  }

  def findForNode(
      mid: MuseumId,
      nodeId: StorageNodeId,
      collectionIds: Seq[MuseumCollection]
  )(
      implicit currUsr: AuthenticatedUser
  ): Future[MusitResult[Seq[EnrichedSampleObject]]] = {
    soDao.listForNode(mid, nodeId, collectionIds)
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
      orig <- MusitResultT(findById(oid, MusitNotFound(s"Couldn't find sample $oid")))
      del  <- MusitResultT(soDao.update(enrich(orig)))
    } yield del

    updatedRes.value
  }

}
