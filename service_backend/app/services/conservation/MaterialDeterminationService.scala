package services.conservation

import com.google.inject.Inject
import models.conservation.events.{MaterialDetermination, Treatment}
import models.conservation._
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models.MuseumCollections.{
  Archeology,
  Collection,
  Ethnography,
  Numismatics
}
import no.uio.musit.models.{CollectionUUID, MuseumId}
import repositories.conservation.dao.{MaterialDeterminationDao, TreatmentDao}

import scala.concurrent.ExecutionContext

class MaterialDeterminationService @Inject()(
    implicit
    override val dao: MaterialDeterminationDao,
    override val ec: ExecutionContext
) extends ConservationEventService[MaterialDetermination] {

  def getArchaeologyMaterialList(
      ): FutureMusitResult[Seq[MaterialArchaeology]] = {
    dao.getArchaeologyMaterialList
  }

  def getEthnographyMaterialList(
      ): FutureMusitResult[Seq[MaterialEthnography]] = {
    dao.getEthnographyMaterialList
  }

  def getNumismaticMaterialList(
      ): FutureMusitResult[Seq[MaterialNumismatic]] = {
    dao.getNumismaticMaterialList
  }

}
