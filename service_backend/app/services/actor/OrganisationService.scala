package services.actor

import com.google.inject.Inject
import models.actor.Organisation
import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.models.OrgId
import no.uio.musit.service.MusitSearch
import repositories.actor.dao.OrganisationDao

import scala.concurrent.Future

class OrganisationService @Inject()(val orgDao: OrganisationDao) {

  def find(id: OrgId): Future[Option[Organisation]] = {
    orgDao.getById(id)
  }

  def find(search: MusitSearch): Future[Seq[Organisation]] = {
    val searchString = search.searchStrings.reduce(_ + " " + _)
    search.searchMap
      .get("tags")
      .map { t =>
        orgDao.getByNameAndTags(searchString, t)
      }
      .getOrElse(orgDao.getByName(searchString))
  }

  def create(org: Organisation): Future[Organisation] = {
    orgDao.insert(org)
  }

  def update(org: Organisation): Future[MusitResult[Option[Int]]] = {
    orgDao.update(org)
  }

  def remove(id: OrgId): Future[Int] = {
    orgDao.delete(id)
  }

  def getAnalysisLabs: Future[MusitResult[Seq[Organisation]]] = orgDao.getAnalysisLabList

}
