package no.uio.musit.microservices.common.log

import java.net.URLEncoder

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.{LayoutBase, UnsynchronizedAppenderBase}
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
      "text" -> slackFormat(layout.doLayout(eventObject)),
      "parse" -> "full",
      "link_names" -> "1",
      "unfurl_links" -> "true",
      "unfurl_media" -> "true"
    )).map{ response =>
      response.status match {
        case 200 => Some(response.json)
        case _ => {
          println("Slack integration down!!! ($webhook)")
          None
        }
      }
    }
  }
}
