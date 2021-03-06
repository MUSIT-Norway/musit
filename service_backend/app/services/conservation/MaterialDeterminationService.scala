package services.conservation

import com.google.inject.Inject
import models.conservation.events.{MaterialDetermination, MaterialInfo, Treatment}
import models.conservation._
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models.MuseumCollections.{
  Archeology,
  Collection,
  Ethnography,
  Numismatics
}
import no.uio.musit.models.{CollectionUUID, MuseumId, ObjectUUID}
import repositories.conservation.dao.{MaterialDeterminationDao, TreatmentDao}

import scala.concurrent.ExecutionContext

class MaterialDeterminationService @Inject()(
    implicit
    override val dao: MaterialDeterminationDao,
    val consService: ConservationService,
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

  def getArchaeologyMaterial(materialId: Int): FutureMusitResult[MaterialArchaeology] = {
    dao.getArchaeologyMaterial(materialId)
  }

  def getEthnographyMaterial(materialId: Int): FutureMusitResult[MaterialEthnography] = {
    dao.getEthnographyMaterial(materialId)
  }

  def getNumismaticMaterial(materialId: Int): FutureMusitResult[MaterialNumismatic] = {
    dao.getNumismaticMaterial(materialId)
  }

  def getCurrentMaterial(
      mid: MuseumId,
      oUuid: ObjectUUID
  ): FutureMusitResult[Seq[MaterialInfo]] = {
    dao
      .getCurrentEventForSpecificEventType(oUuid, MaterialDetermination.eventTypeId)
      .flatMap {
        case Some(m) => dao.getSpecialAttributes(m)
        case None    => FutureMusitResult.successful(Seq.empty)
      }
  }

}
