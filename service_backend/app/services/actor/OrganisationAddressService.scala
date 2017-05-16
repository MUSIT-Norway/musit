package services.actor

import com.google.inject.Inject
import models.actor.OrganisationAddress
import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.models.{DatabaseId, OrgId}
import repositories.actor.dao.AddressDao

import scala.concurrent.Future

class OrganisationAddressService @Inject()(val adrDao: AddressDao) {

  def all(organizationId: OrgId): Future[Seq[OrganisationAddress]] = {
    adrDao.allFor(organizationId)
  }

  def find(orgId: OrgId, id: DatabaseId): Future[Option[OrganisationAddress]] = {
    adrDao.getById(orgId, id)
  }

  def create(orgId: OrgId, address: OrganisationAddress): Future[OrganisationAddress] = {
    adrDao.insert(address.copy(organisationId = Some(orgId)))
  }

  def update(address: OrganisationAddress): Future[MusitResult[Option[Int]]] = {
    adrDao.update(address)
  }

  def remove(id: DatabaseId): Future[Int] = {
    adrDao.delete(id)
  }

}
