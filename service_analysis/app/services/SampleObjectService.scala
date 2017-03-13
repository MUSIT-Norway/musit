package services

import com.google.inject.Inject
import models.SampleObject
import no.uio.musit.MusitResults.{MusitError, MusitResult, MusitSuccess}
import no.uio.musit.models.ObjectUUID
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.dao.SampleObjectDao

import scala.concurrent.Future

class SampleObjectService @Inject() (
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
    soDao.insert(sobj)
  }

  def update(
    oid: ObjectUUID,
    so: SampleObject
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[Option[SampleObject]]] = {
    val sobj = so.copy(
      updatedBy = Some(currUser.id),
      updatedDate = Some(dateTimeNow)
    )

    soDao.update(sobj).flatMap {
      case MusitSuccess(nu) => soDao.findByUUID(oid)
      case err: MusitError => Future.successful(err)
    }
  }

  def findById(oid: ObjectUUID): Future[MusitResult[Option[SampleObject]]] = {
    soDao.findByUUID(oid)
  }

  def findForParent(oid: ObjectUUID): Future[MusitResult[Seq[SampleObject]]] = {
    soDao.listForParentObject(oid)
  }

}
