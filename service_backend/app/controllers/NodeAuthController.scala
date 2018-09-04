package controllers

import no.uio.musit.MusitResults._
import no.uio.musit.functional.FutureMusitResult
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{Result, Results}

import no.uio.musit.MusitResults.{
  MusitError,
  MusitResult,
  MusitSuccess,
  MusitValidationError
}
import no.uio.musit.models.{MuseumId, Museums}
import no.uio.musit.security.ModuleConstraint
import no.uio.musit.security.Permissions.Permission
import play.api.mvc.{Action, AnyContent}

import scala.concurrent.Future
import com.google.inject.{Inject, Singleton}
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import play.api.mvc.ControllerComponents

@Singleton
class NodeAuthController @Inject()(
    implicit
    val controllerComponents: ControllerComponents,
    val authService: Authenticator
) extends MusitController {

  /**
   * This is to be called from the TypeScript backend to authorize stuff, until we have a complete auth implementation in the TypeScript backend.
   * Note that we also need to add checks on collection type in the future, this is a serious
   * hole in the implementation of authorization in MusitSecureAction.
   *
   */
  def authorize(
      museumId: Option[Int],
      moduleConstraintId: Option[Int], //  Option[ModuleConstraint],
      permissions: Seq[Int]
  ) = {

    val resOptMuseum = museumId match {
      case Some(musId) =>
        val optFoundMuseum = Museums.museums.find(m => m.id == MuseumId(musId))
//        println(s"optFoundMuseum: $optFoundMuseum")
        optFoundMuseum match {
          case Some(x) => MusitSuccess(Some(x))
          case None    => MusitValidationError(s"Undefined Museum id: $musId")
        }
      case None => MusitSuccess(None)
    }

    val resOptModule = moduleConstraintId match {
      case Some(m) =>
        ModuleConstraint.fromInt(m) match {
          case Some(c) => MusitSuccess(Some(c))
          case None    => MusitValidationError(s"Undefined ModuleConstraint id: $m")
        }
      case None => MusitSuccess(None)
    }

    val permissionsResults = permissions.map { p =>
      val perm = Permission.fromInt(p)
      val permResult =
        if (perm.priority == 0 && p != 0)
          MusitValidationError(s"undefined permission id: $p")
        else MusitSuccess(perm)
      permResult
    }

    val resPermissions = MusitResult.sequence(permissionsResults)
//    println(s"permissions: $resPermissions")

    val res = for {
      optMuseum   <- resOptMuseum
      optModule   <- resOptModule
      permissions <- resPermissions

    } yield
      MusitSecureAction(optMuseum.map(_.id), optModule, permissions: _*).async {
        implicit request =>
          Future.successful(Ok)
      }
    val result = res match {
      case MusitSuccess(x: Action[AnyContent]) => x
      case err: MusitError => {
//        println(s"MusitError: ${err.message}")
        Action { request =>
          MusitResultUtils.musitErrorToPlayResult(err)
        }
      }
    }
    result
  }
}
