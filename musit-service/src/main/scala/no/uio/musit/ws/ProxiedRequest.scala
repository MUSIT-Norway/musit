package no.uio.musit.ws

import no.uio.musit.ws.ProxiedRequest._
import play.api.Configuration
import play.api.libs.ws.{DefaultWSProxyServer, WSProxyServer, WSRequest}

class ProxiedRequest(req: WSRequest, config: Configuration) {

  val proxy: Option[WSProxyServer] = for {
    host <- config.getString(ProxyHost)
    port <- config.getInt(ProxyPort)
    user     = config.getString(ProxyUser)
    password = config.getString(ProxyPassword)
  } yield
    DefaultWSProxyServer(host = host, port = port, principal = user, password = password)

  def viaProxy: WSRequest = proxy.map(req.withProxyServer).getOrElse(req)

}

object ProxiedRequest {

  val ProxyHost     = "ws.proxy.host"
  val ProxyPort     = "ws.proxy.port"
  val ProxyUser     = "ws.proxy.user"
  val ProxyPassword = "ws.proxy.password"

}

trait ViaProxy {

  implicit def viaProxy(req: WSRequest)(implicit config: Configuration): ProxiedRequest =
    new ProxiedRequest(req, config)

}

object ViaProxy extends ViaProxy
