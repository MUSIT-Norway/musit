package no.uio.musit

import play.api.libs.json.{JsError, JsResult, JsSuccess}

import scala.util.{Failure, Success, Try}

object MusitResults {

  sealed abstract class MusitResult[+A] {

    def isSuccess: Boolean

    def isFailure: Boolean = !isSuccess

    def get: A

    def toOption: Option[A] = if (isFailure) None else Option(this.get)

    def map[B](f: A => B): MusitResult[B] = this match {
      case MusitSuccess(success) => MusitSuccess(f(success))
      case err: MusitError       => err
    }

    def flatMap[B](f: A => MusitResult[B]): MusitResult[B] = this match {
      case MusitSuccess(success) => f(success)
      case err: MusitError       => err
    }

    def flatten[B](implicit ev: A <:< MusitResult[B]): MusitResult[B] = {
      this match {
        case MusitSuccess(success) => success
        case err: MusitError       => err
      }
    }

    final def getOrElse[B >: A](default: => B): B = {
      if (isFailure) default else this.get
    }

  }

  object MusitResult {

    /**
     * Helper function to create a MusitResult based on the option
     * value. It will return MusitSuccess if it's present and a provided
     * MusitError if not.
     */
    def getOrError[T](opt: Option[T], err: MusitError) =
      opt.map(MusitSuccess.apply).getOrElse(err)

    private def handleSequenceErrors(failures: Seq[MusitError]) = {
      if (failures.size == 1) {
        failures.head
      } else {
        val validationErrors = failures.collect {
          case valError: MusitValidationError => valError
        }
        if (validationErrors.size == failures.size) {
          MusitValidationError(
            s"${validationErrors.size} error(s). First error was: ${validationErrors.head}"
          )
        } else
          MusitGeneralError(
            s"${validationErrors.size} error(s). First error was: ${validationErrors.head}"
          )

      }
    }

    /**
     * Takes a {{{Seq[MusitResult[T]]}}} and flips it to a {{{Seq[MusitResult[T]]}}}
     *
     * @param seq A collection of MusitResults
     * @tparam T the type encapsulated in MusitResult
     * @return A MusitResult with a collection of T's
     */
    def sequence[T](seq: Seq[MusitResult[T]]): MusitResult[Seq[T]] = {

      if (seq.exists(_.isFailure)) {
        // If the list contains any errors at all, we bail the processing and
        // calculates an appropriate error.
        val failures = seq.collect { case err: MusitError => err }
        handleSequenceErrors(failures)

      } else {
        // We now _know_ all entries in the list are of type MusitSuccess
        seq.foldLeft[MusitResult[Seq[T]]](MusitSuccess(Seq.empty[T])) {
          case (acc, curr) =>
            curr match {
              case MusitSuccess(value) => acc.map(_ :+ value)
              case err: MusitError     => err
            }
        }
      }
    }

    def fromTry[T](
        value: Try[T],
        errorFactory: Throwable => MusitError
    ): MusitResult[T] = {
      value match {
        case Success(t)  => MusitSuccess(t)
        case Failure(ex) => errorFactory(ex)
      }
    }

    def fromJsResult[A](jsr: JsResult[A]): MusitResult[A] = {
      jsr match {
        case JsSuccess(a, path) => MusitSuccess(a)
        case JsError(errors)    => MusitValidationError(errors.toString())
      }
    }

  }

  /**
   * Use this to as the return type when an operation is successful and has value.
   */
  case class MusitSuccess[+A](value: A) extends MusitResult[A] {
    override val isSuccess: Boolean = true

    override def get: A = value
  }

  sealed trait MusitError extends MusitResult[Nothing] {
    val message: String

    override def get: Nothing =
      throw new NoSuchElementException("MusitResult.get on MusitError")
  }

  // ========================================================================
  // Specific error types inheriting from MusitError
  // ========================================================================

  /**
   * A special result case to represent a status where there was no argument.
   * It's really a success state, but it handles better in the code if treated
   * here as a MusitError type.
   */
  case object MusitEmpty extends MusitError {
    override val message: String    = "empty"
    override val isSuccess: Boolean = false
  }

  /**
   * This can be used when data is not found.
   */
  case class MusitNotFound(message: String) extends MusitError {
    override val isSuccess: Boolean = false
  }

  /**
   * Use this when validation of fields and conditions are not met.
   */
  case class MusitValidationError(
      message: String,
      expected: Option[Any] = None,
      actual: Option[Any] = None
  ) extends MusitError {
    override val isSuccess: Boolean = false
  }

  /**
   * Use this when dealing with unexpected internal errors.
   */
  case class MusitInternalError(message: String) extends MusitError {
    override val isSuccess: Boolean = false
  }

  /**
   * Use this error type when handling Exceptions from the DB driver.
   */
  case class MusitDbError(
      message: String,
      ex: Option[Throwable] = None
  ) extends MusitError {
    override val isSuccess: Boolean = false
  }

  /**
   * Use this error type when handling situations where authentication is
   * required. Typically this will in an Action or at least near the controller
   * or service implementation.
   */
  case class MusitNotAuthenticated(
      message: String = "Requires authentication"
  ) extends MusitError {
    override val isSuccess: Boolean = false
  }

  /**
   * Use this error type whenever some level of authorization is required.
   */
  case class MusitNotAuthorized() extends MusitError {
    override val message: String    = "Requires authorization"
    override val isSuccess: Boolean = false
  }

  /**
   * In case of a general error situation that needs handling.
   */
  case class MusitGeneralError(message: String) extends MusitError {
    override val isSuccess: Boolean = false
  }

  /**
   * Use when communication with remote HTTP services fail.
   */
  case class MusitHttpError(status: Int, message: String) extends MusitError {
    override val isSuccess: Boolean = false
  }
}
