package no.uio.musit.microservice.storageAdmin.service

import com.google.inject.Inject
import no.uio.musit.microservice.storageAdmin.dao.OrganisationDao
import no.uio.musit.microservice.storageAdmin.domain.dto.StorageNodeDTO
import no.uio.musit.microservice.storageAdmin.domain.{ Organisation, Storage }
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ServiceHelper

class OrganisationService @Inject() (organisationDao: OrganisationDao) {
  def create(storageUnit: StorageNodeDTO, storageOrganisation: Organisation): MusitFuture[Storage] =
    ServiceHelper.daoInsert(organisationDao.insertOrganisation(storageUnit, storageOrganisation))

  def updateOrganisationByID(id: Long, organisation: Organisation) =
    organisationDao.updateOrganisation(id, organisation)

  def getOrganisationById(id: Long) =
    organisationDao.getOrganisationById(id)
}
