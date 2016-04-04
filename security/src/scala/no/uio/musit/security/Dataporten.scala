package no.uio.musit.security

import no.uio.musit.security.dataporten.RawServices
import play.api.libs.json._
import play.api.libs.ws.WSRequest
import play.api.libs.ws.ning.NingWSClient
import play.api.libs.functional.syntax._
import play.api.libs.json

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by jstabel on 3/31/16.
  */


object MusitUtils {

  implicit class MusitNingWSClient(val wsr: WSRequest) extends AnyVal {
    def withBearerToken(token: String) = {
      wsr.withHeaders("Authorization" -> ("Bearer " + token))
    }
  }

}

class Context(val accessToken: String) {
  val ws = NingWSClient()
}

case class UserInfo(id: String, name: String)

case class GroupInfo(groupType: String, id: String, displayName: String , description: Option[String])


import play.api.cache._
import play.api.mvc._
import javax.inject.Inject

object Services /*@Inject() (cache: CacheApi)*/ {


  def getUserInfo(context: Context) = RawServices.getUserInfo(context)

  def getUserGroups(context: Context) = RawServices.getUserGroups(context: Context)

}


object dataporten {
  def createUserInfo(sub: String, name: String) = new UserInfo(sub, name)

  def createGroupInfo(groupType: String, id: String, displayName: String , description: Option[String]) = new GroupInfo(groupType, id, displayName, description)


  val userInfoUrl = "https://auth.feideconnect.no/openid/userinfo"
  val userGroupsUrl = "https://groups-api.feideconnect.no/groups/me/groups"

  implicit val userInfoReads: Reads[UserInfo] = (
    (JsPath \ "sub").read[String] and
      (JsPath \ "name").read[String]
    ) (createUserInfo _)

  implicit val groupInfoReads: Reads[GroupInfo] = (
    (JsPath \ "type").read[String] and
      (JsPath \ "id").read[String] and
      (JsPath \ "displayName").read[String] and
      (JsPath \ "description").readNullable[String]

    ) (createGroupInfo _)

  object RawServices {

    import MusitUtils._

    def httpGet(context: Context, url: String) = {
      context.ws.url(url).withBearerToken(context.accessToken).get()

    }

    def getUserInfo(context: Context) = {
      httpGet(context, userInfoUrl).map(resp => resp.body).map(Json.parse(_).validate[UserInfo].get)
    }

    def getUserGroups(context: Context) = {
      httpGet(context, userGroupsUrl).map(resp => resp.body).map { j => println(j); Json.parse(j).validate[Seq[GroupInfo]].get }
    }
  }

}