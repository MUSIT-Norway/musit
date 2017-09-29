import no.uio.musit.MusitResults.{MusitGeneralError, MusitNotFound}

import scala.concurrent.Future.{successful => evaluated}

package object services {

  def generalErrorF(msg: String) = evaluated(MusitGeneralError(msg))
  def notFoundF(msg: String)     = evaluated(MusitNotFound(msg))

}
