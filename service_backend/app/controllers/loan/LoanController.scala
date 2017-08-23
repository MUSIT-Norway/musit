package controllers.loan

import com.google.inject.Inject
import models.loan.event.SaveCommands.CreateLoanCommand
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.models.MuseumId
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import services.loan.LoanService
import controllers._
import play.api.mvc.ControllerComponents

class LoanController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val loanService: LoanService
) extends MusitController {

  def createLoan(mid: MuseumId) =
    MusitSecureAction().async(parse.json) { implicit request =>
      implicit val currUser = implicitly(request.user)
      val jsr               = request.body.validate[CreateLoanCommand]
      saveRequest[CreateLoanCommand, Long](jsr) { cmd =>
        loanService.createLoan(mid, cmd.toDomain).map(_.map(_.underlying))
      }
    }

  def findActiveLoan(mid: MuseumId) =
    MusitSecureAction().async { implicit request =>
      loanService.findActiveLoans(mid).map {
        case MusitSuccess(res) => listAsPlayResult(res)
        case err: MusitError   => internalErr(err)
      }
    }

}
