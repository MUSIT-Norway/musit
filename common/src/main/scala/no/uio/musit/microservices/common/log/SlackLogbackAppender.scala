package no.uio.musit.microservices.common.log

import java.net.URLEncoder

import scala.concurrent.ExecutionContext.Implicits.global
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.{LayoutBase, UnsynchronizedAppenderBase}
import play.Logger
import play.api.Play.current
import play.api.libs.ws._

object SlackDefaults {
  val layout = new LayoutBase[ILoggingEvent] {
    override def doLayout(event: ILoggingEvent): String = {
      return s"${event.getLevel}] ${event.getLoggerName} - ${event.getFormattedMessage()}"
    }
  }
}

class SlackLogbackAppender extends UnsynchronizedAppenderBase[ILoggingEvent] {
  var webhook = ""
  var host = ""
  var service = ""
  var layout = SlackDefaults.layout

  def slackFormat(msg:String) : String = {
    return URLEncoder.encode(s"[$host:$service:${msg.replaceAll("\n\t", "\n")}", "UTF-8")
  }

  override def append(eventObject: ILoggingEvent): Unit = {
    val future = WS.url(webhook).post(Map(
      "text" -> Seq(slackFormat(layout.doLayout(eventObject))),
      "parse" -> Seq("full"),
      "link_names" -> Seq("1"),
      "unfurl_links" -> Seq("true"),
      "unfurl_media" -> Seq("true")
    )).map{ response =>
      response.status match {
        case 200 => Some(response.json)
        case _ => {
          Logger.error("Slack integration down!!! ($webhook)")
          None
        }
      }
    }
  }
}
