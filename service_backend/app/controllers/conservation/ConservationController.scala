package controllers.conservation

import com.google.inject.{Inject, Singleton}
import controllers.{internalErr, _}
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.models.{CollectionUUID, MuseumId}
import no.uio.musit.security.Permissions.Read
import no.uio.musit.security.{Authenticator, CollectionManagement}
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.mvc.ControllerComponents
import MusitResultUtils._
import services.conservation.{
  ConservationProcessService,
  ConservationService,
  TreatmentService
}
@Singleton
class ConservationController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val cpService: ConservationProcessService,
    val conservationService: ConservationService,
    val treatmentService: TreatmentService
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

  def getMaterialList =
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

}
