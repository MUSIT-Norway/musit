package controllers.musitsearchobject

import com.google.inject.Inject
import controllers.MusitResultUtils._
import controllers._
import models.musitsearchobject.SearchObjectResult._
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models._
import no.uio.musit.security.Permissions.{GodMode, Read}
import no.uio.musit.security.{AccessAll, AuthenticatedUser, Authenticator}
import no.uio.musit.service.MusitController
import play.api.mvc.ControllerComponents
import play.api.{Configuration, Logger}
import services.musitsearchobject.SearchObjectService

import scala.concurrent.Future

class SearchObjectController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val conf: Configuration,
    val searchService: SearchObjectService
) extends MusitController {

  //These are copied from ObjectController. These config options doesn't seem to be set at the moment though.

  val maxLimitConfKey     = "musit.objects.search.max-limit"
  val defaultLimitConfKey = "musit.objects.search.default-limit"

  val logger = Logger(classOf[SearchObjectController])

  private val maxLimit     = conf.getOptional[Int](maxLimitConfKey).getOrElse(10000)
  private val defaultLimit = conf.getOptional[Int](defaultLimitConfKey).getOrElse(25)

  private def calcLimit(l: Int): Int = l match {
    case lim: Int if lim > maxLimit => maxLimit
    case lim: Int if lim < 0        => defaultLimit
    case lim: Int                   => lim
  }

  def search(
      mid: Int,
      collectionIds: String,
      from: Int,
      limit: Int = defaultLimit,
      museumNo: Option[String],
      museumNoAsANumber: Option[String],
      subNo: Option[String],
      term: Option[String],
      q: Option[String],
      ignoreSamples: Boolean
  ) = MusitSecureAction(mid, Read).async { implicit request =>
    implicit val currUser: AuthenticatedUser = request.user
    parseCollectionIdsParam(mid, AccessAll, collectionIds) match {
      case Left(res) => Future.successful(res)
      case Right(cids) =>
        val mno      = museumNo.map(MuseumNo.apply)
        val sno      = subNo.map(SubNo.apply)
        val pageSize = calcLimit(limit)
        val pageNr   = (from / pageSize) + 1

        futureMusitResultToPlayResult(
          searchService.findObjects(
            mid,
            mno,
            museumNoAsANumber,
            sno,
            term,
// add later? q,
            cids,
            pageNr,
            pageSize
//add later?            ignoreSamples
          )
        )
    }
  }

  def reindexSearchDb(
      ) = MusitSecureAction(GodMode).async { implicit request =>
    implicit val currUser: AuthenticatedUser = request.user
    val res                                  = FutureMusitResult.from(searchService.recreateSearchTable())
    futureMusitResultUnitToPlayResult(res)
  }
}
