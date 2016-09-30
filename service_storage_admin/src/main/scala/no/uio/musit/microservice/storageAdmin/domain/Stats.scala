package no.uio.musit.microservice.storageAdmin.domain

import play.api.libs.json.Json

/**
 * Created by jarle on 22.09.16.
 */
case class Stats(nodes: Int, objects: Int, totalObjects: Int)

object Stats {
  implicit val format = Json.format[Stats]

}
