package no.uio.musit.healthcheck

import java.io.File
import java.nio.file.Files

import org.joda.time.DateTime
import play.api.libs.json._

/**
 * @param name A name that should be the same for the environment
 * @param instance Unique name for the instance.
 * @param url Endpoint to verify that the application is up.
 *            Accepts http codes 200 and 304
 * @param hostGroup The group that zabbix can send notifications to.
 */
case class ZabbixMeta(
    name: String,
    instance: String,
    url: String,
    hostGroup: String
)

case class ZabbixFile(dir: String, name: String) {
  def ensureWritableFile(): File = {
    Files.createDirectories(new File(dir).toPath)
    val zabbixFile = new File(dir, name)
    zabbixFile.createNewFile()
    zabbixFile
  }
}

case class Zabbix(
    meta: ZabbixMeta,
    updated: DateTime,
    healthChecks: Set[HealthCheckStatus]
) {

  def toJson = {
    val metaJson = Map(
      "zabbix-name" -> JsString(meta.name),
      "instance"    -> JsString(meta.instance),
      "url"         -> JsString(meta.url),
      "host-group"  -> JsString(meta.hostGroup),
      "updated"     -> JsNumber(updated.getMillis) //todo: timezone?
    )
    val checksJson = healthChecks.map(hc => hc.name -> JsBoolean(hc.available)).toMap
    JsObject(metaJson ++ checksJson)
  }

}
