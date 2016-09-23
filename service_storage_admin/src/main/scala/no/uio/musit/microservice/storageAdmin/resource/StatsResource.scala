package no.uio.musit.microservice.storageAdmin.resource

/**
 * Created by jarle on 13.09.16.
 */

import com.google.inject.Inject
import no.uio.musit.microservice.storageAdmin.domain.Stats
import no.uio.musit.microservice.storageAdmin.service._
import no.uio.musit.microservices.common.utils.ResourceHelper
import play.api.libs.json._
import play.api.mvc._

class StatsResource @Inject() (
    statsService: StatsService
) extends Controller {

  def getStats(nodeId: Long) = Action.async {
    ResourceHelper.getRoot(statsService.getStats(nodeId), (n: Stats) => Json.toJson(n))
  }
}
