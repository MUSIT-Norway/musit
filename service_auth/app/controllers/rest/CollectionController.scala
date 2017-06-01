package controllers.rest

import com.google.inject.Inject
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.security.Authenticator
import no.uio.musit.security.Permissions.{GodMode, MusitAdmin}
import no.uio.musit.security.crypto.MusitCrypto
import no.uio.musit.service.MusitAdminController
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._
import repositories.actor.dao.AuthDao

class CollectionController @Inject()(
    implicit val authService: Authenticator,
    val crypto: MusitCrypto,
    val dao: AuthDao
) extends MusitAdminController {

  val logger = Logger(classOf[CollectionController])

  private def serverError(msg: String): Result =
    InternalServerError(Json.obj("message" -> msg))

  /**
   * Fetch all MuseumCollections in the system
   */
  def getAllCollections = MusitSecureAction().async { implicit request =>
    dao.allCollections.map {
      case MusitSuccess(cols) => if (cols.nonEmpty) Ok(Json.toJson(cols)) else NoContent
      case err: MusitError    => serverError(err.message)
    }
  }

  def getCollection(
      colId: String
  ) = MusitSecureAction().async { implicit request =>
    ???
  }

  def addCollection =
    MusitAdminAction(MusitAdmin).async(parse.json) { implicit request =>
      ???
    }

  def updateCollection(
      colId: String
  ) = MusitAdminAction(MusitAdmin).async(parse.json) { implicit request =>
    ???
  }

  def removeCollection(
      colId: String
  ) = MusitAdminAction(GodMode).async { implicit request =>
    ???
  }

}
