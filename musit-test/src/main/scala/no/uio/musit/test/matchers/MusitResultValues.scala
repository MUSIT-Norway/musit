package no.uio.musit.test.matchers

import no.uio.musit.MusitResults.{MusitDbError, MusitError, MusitResult}
import org.scalatest.exceptions.TestFailedException

import scala.reflect.{ClassTag, classTag}

/**
 * A trait that returns the value of the MusitResult or fails the test. This is
 * to reduce the amount of get calls in our code and give us better fault messages
 * when the value isn't present.
 *
 * Inspired by OptionValues.
 *
 * @see [[org.scalatest.OptionValues]]
 */
trait MusitResultValues {

  implicit def convertMusitResultToValuable[T: ClassTag](
      res: MusitResult[T]
  ): Valuable[T] =
    new Valuable(res)

  class Valuable[T: ClassTag](res: MusitResult[T]) {

    def successValue: T = {
      if (res.isSuccess) res.get
      else testFailedException
    }

    private def testFailedException = {
      val className = classTag[T].runtimeClass.getSimpleName
      throw new TestFailedException(
        Some(s"Expected a MusitSuccess[$className] but found $res"),
        res match {
          case err: MusitDbError => err.ex
          case _                 => None
        },
        0
      )
    }
  }

}

object MusitResultValues extends MusitResultValues
