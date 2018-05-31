package controllers.conservation

import com.google.inject.{Inject, Singleton}
import controllers.MusitResultUtils._
import controllers.{internalErr, parseCollectionIdsParam}
import no.uio.musit.MusitResults.{MusitError, MusitSuccess, MusitValidationError}
import no.uio.musit.models.MuseumCollections.{
  Archeology,
  Collection,
  Ethnography,
  Numismatics
}
import no.uio.musit.models.{CollectionUUID, MuseumId, ObjectUUID}
import no.uio.musit.security.Permissions.Read
import no.uio.musit.security.{
  AccessAll,
  AuthenticatedUser,
  Authenticator,
  CollectionManagement
}
import no.uio.musit.service.MusitController
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.{Configuration, Logger}
import services.conservation._
import services.elasticsearch.search.ConservationSearchService

import scala.concurrent.Future
@Singleton
class ConservationController @Inject()(
    val conf: Configuration,
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val cpService: ConservationProcessService,
    val conservationService: ConservationService,
    val treatmentService: TreatmentService,
    val conditionAssessmentService: ConditionAssessmentService,
    val materialDeterminationService: MaterialDeterminationService,
    val measurementDeterminationService: MeasurementDeterminationService,
    val conservationSearchService: ConservationSearchService
) extends MusitController {

  val logger = Logger(classOf[ConservationController])

  val maxLimitConfKey     = "musit.conservation.search.max-limit"
  val defaultLimitConfKey = "musit.conservation.search.default-limit"

  private val maxLimit     = conf.getOptional[Int](maxLimitConfKey).getOrElse(100)
  private val defaultLimit = conf.getOptional[Int](defaultLimitConfKey).getOrElse(25)

  private def calcLimit(l: Int): Int = l match {
    case lim: Int if lim > maxLimit => maxLimit
    case lim: Int if lim < 0        => defaultLimit
    case lim: Int                   => lim
  }

  def getConservationTypes(
      mid: MuseumId,
      collectionIds: Option[String]
  ) =
    MusitSecureAction(mid).async { implicit request =>
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

  def search(
      mid: Int,
      collectionIds: String,
      q: Option[String],
      from: Int,
      limit: Int
  ) =
    MusitSecureAction().async { implicit request =>
      implicit val currUser: AuthenticatedUser = request.user

      parseCollectionIdsParam(mid, AccessAll, collectionIds) match {
        case Left(res) => Future.successful(res)
        case Right(collIds) =>
          conservationSearchService
            .restrictedConservationSearch(
              mid = MuseumId(mid),
              collectionIds = collIds,
              from = from,
              limit = calcLimit(limit),
              queryStr = q
            )
            .map {
              case MusitSuccess(res) =>
                Ok(res.raw)

              case err: MusitError =>
                logger.error(err.message)
                internalErr(err)
            }
      }
    }
}
