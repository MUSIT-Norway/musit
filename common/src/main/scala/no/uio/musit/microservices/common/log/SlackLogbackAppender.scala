package no.uio.musit.microservices.common.log

import java.net.URLEncoder

import scala.concurrent.ExecutionContext.Implicits.global
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.{LayoutBase, UnsynchronizedAppenderBase}
import play.Logger
import play.api.Play.current
import play.api.libs.json.{JsNumber, JsObject, JsString}
import play.api.libs.ws._

object SlackDefaults {
  val layout = new LayoutBase[ILoggingEvent] {
    override def doLayout(event: ILoggingEvent): String = {
      s"${event.getLevel}] ${event.getLoggerName} - ${event.getFormattedMessage()}"
    }
  }
}

class SlackLogbackAppender extends UnsynchronizedAppenderBase[ILoggingEvent] {
  var webhook = ""
  var host = ""
  var service = ""
  var layout = SlackDefaults.layout
  Logger.debug(s"Started slack appender ($webhook, $host, $service)")

  def setWebhook(input:String) = {
    webhook = input
  }

  def setHost(input:String) = {
    host = input
  }

  def setService(input:String) = {
    service = input
  }

  def setLayout(input: LayoutBase[ILoggingEvent]) = {
    layout = input
  }

  def slackFormat(msg:String) : String = {
    s"[$host:$service:${msg.replaceAll("\n\t", "\n")}"
  }


  override def append(eventObject: ILoggingEvent): Unit = {
    val json = JsObject(Seq(
      "text" -> JsString(slackFormat(layout.doLayout(eventObject))),
      "username" -> JsString(service),
      "parse" -> JsString("full"),
      "link_names" -> JsNumber(1),
      "unfurl_links" -> JsString("true"),
      "unfurl_media" -> JsString("true")
    ))
    Logger.debug(s"Sending message to slack: $json")
    val future = WS.url(webhook).post(json).map{ response =>
      response.status match {
        case 200 => {
          Logger.debug(s"Sent error to slack: ${response.json}")
          Some(response.json)
        }
        case err => {
          Logger.error(s"Slack integration down!!! (${this.webhook}) - $err: ${response.body}")
          None
        }
      }
    }
  }
}