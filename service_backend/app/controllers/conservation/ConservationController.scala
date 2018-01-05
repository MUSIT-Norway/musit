package controllers.conservation

import com.google.inject.{Inject, Singleton}
import controllers.conservation.MusitResultUtils._
import no.uio.musit.MusitResults.MusitValidationError
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models.MuseumCollections.{
  Archeology,
  Collection,
  Ethnography,
  Numismatics
}
import no.uio.musit.models.{CollectionUUID, MuseumId, ObjectUUID}
import no.uio.musit.security.Permissions.Read
import no.uio.musit.security.{Authenticator, CollectionManagement}
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import services.conservation._

import scala.concurrent.Future
@Singleton
class ConservationController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val cpService: ConservationProcessService,
    val conservationService: ConservationService,
    val treatmentService: TreatmentService,
    val conditionAssessmentService: ConditionAssessmentService,
    val materialDeterminationService: MaterialDeterminationService,
    val measurementDeterminationService: MeasurementDeterminationService
) extends MusitController {

  val logger = Logger(classOf[ConservationController])

  def getConservationTypes(
      mid: MuseumId,
      collectionIds: Option[String]
  ) =
    MusitSecureAction(mid, CollectionManagement, Read).async { implicit request =>
      implicit val currUser = request.user

      val maybeColl = collectionIds.flatMap(CollectionUUID.fromString)
      futureMusitResultSeqToPlayResult(cpService.getTypesFor(maybeColl))
    /* .map {
        case MusitSuccess(types) => listAsPlayResult(types)
        case err: MusitError     => internalErr(err)
      }*/
    }

  def getTreatmentMaterialList =
    MusitSecureAction().async { implicit request =>
      futureMusitResultSeqToPlayResult(treatmentService.getMaterialList)
    }

  def getKeywordList =
    MusitSecureAction().async { implicit request =>
      futureMusitResultSeqToPlayResult(treatmentService.getKeywordList)
    }

  def getRoleList = MusitSecureAction().async { implicit request =>
    futureMusitResultSeqToPlayResult(conservationService.getRoleList)
  }

  def getConditionCodeList = MusitSecureAction().async { implicit request =>
    futureMusitResultSeqToPlayResult(conditionAssessmentService.getConditionCodeList)
  }

  def getMaterialList(mid: MuseumId, collectionId: String) = {
    MusitSecureAction().async { implicit request =>
      val collectionUuid = CollectionUUID.unsafeFromString(collectionId)
      Collection.fromCollectionUUID(collectionUuid) match {
        case Archeology =>
          futureMusitResultSeqToPlayResult(
            materialDeterminationService.getArchaeologyMaterialList
          )
        case Ethnography =>
          futureMusitResultSeqToPlayResult(
            materialDeterminationService.getEthnographyMaterialList
          )
        case Numismatics =>
          futureMusitResultSeqToPlayResult(
            materialDeterminationService.getNumismaticMaterialList
          )
        case _ =>
          MusitValidationError(
            s"Unable to find a materialList for this collection: $collectionUuid "
          ).toFuturePlayResult
      }
    }
  }

  def getMaterial(materialId: Int, collectionId: String) = {
    MusitSecureAction().async { implicit request =>
      val collectionUuid = CollectionUUID.unsafeFromString(collectionId)
      Collection.fromCollectionUUID(collectionUuid) match {
        case Archeology =>
          futureMusitResultToPlayResult(
            materialDeterminationService.getArchaeologyMaterial(materialId)
          )
        case Ethnography =>
          futureMusitResultToPlayResult(
            materialDeterminationService.getEthnographyMaterial(materialId)
          )
        case Numismatics =>
          futureMusitResultToPlayResult(
            materialDeterminationService.getNumismaticMaterial(materialId)
          )
        case _ =>
          MusitValidationError(
            s"Unable to find the materialId: $materialId for this collection: $collectionUuid "
          ).toFuturePlayResult
      }
    }
  }

  /** Return materialId, MaterialExtras from current MaterialDetermination event for a specific object.
   * This is a generic method so collectionId is not needed. */
  def getCurrentMaterialDataForObject(mid: MuseumId, oid: String) =
    MusitSecureAction(mid, CollectionManagement, Read).async { implicit request =>
      implicit val currUser = request.user
      ObjectUUID
        .fromString(oid)
        .map { oUuid =>
          futureMusitResultSeqToPlayResult(
            materialDeterminationService.getCurrentMaterial(mid, oUuid)
          )
        }
        .getOrElse {
          Future.successful(
            BadRequest(Json.obj("message" -> s"Invalid object UUID $oid"))
          )
        }
    }

  def getCurrentMeasurementDataForObject(mid: MuseumId, oid: String) =
    MusitSecureAction(mid, CollectionManagement, Read).async { implicit request =>
      implicit val currUser = request.user
      ObjectUUID
        .fromString(oid)
        .map { oUuid =>
          futureMusitResultToPlayResult(
            measurementDeterminationService.getCurrentMeasurement(mid, oUuid)
          )
        }
        .getOrElse {
          Future.successful(
            BadRequest(Json.obj("message" -> s"Invalid object UUID $oid"))
          )
        }
    }

}
