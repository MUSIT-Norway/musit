package no.uio.musit.security

import play.Logger
import play.api.libs.json._
import play.api.libs.ws.WSRequest
import play.api.libs.ws.ning.{NingWSClient, NingWSResponse}
import play.api.libs.functional.syntax._
import play.api.libs.json

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/**
  * Created by jstabel on 3/31/16.
  */

class Context(val accessToken: String) {
  val ws = NingWSClient()
}

import play.api.cache._
import play.api.mvc._
import javax.inject.Inject


object dataporten {
  def createUserInfo(sub: String, name: String) = new UserInfo(sub, name)

  def createGroupInfo(groupType: String, id: String, displayName: String, description: Option[String]) = new GroupInfo(groupType, id, displayName, description)


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

  class DataportenUserInfoProvider(accessToken: String) extends ConnectionInfoProvider {
    val ws = NingWSClient()

    import no.uio.musit.microservices.common.extensions.PlayExtensions._

    def httpGet(url: String) = {
      ws.url(url).withBearerToken(accessToken).getOrFail()

    }

    def getUserInfo = {
      //httpGet(userInfoUrl).map(resp => resp.body).map(Json.parse(_).validate[UserInfo].get)
      httpGet(userInfoUrl).onComplete {

        case Success(s) => println(s"getUserInfo: Success=$s")
        case Failure(s) => println(s"getUserInfo:failure: ${s.getMessage}")
      }
      val fut1 = httpGet(userInfoUrl).map { resp =>
        println(s"Inni map fut1:${resp.statusText} body: ${resp.body}")
        resp.body
      }
      fut1.map(Json.parse(_).validate[UserInfo].get)
    }

    def getUserGroups = {
      httpGet(userGroupsUrl).map(resp => resp.body).map { j => /*println(j);*/ Json.parse(j).validate[Seq[GroupInfo]].get }
    }
  }


  class DataportenSecurityConnection(userInfo: UserInfo, userGroups: Seq[String]) extends SecurityConnectionBaseImp(userInfo.name, userGroups) {
    override def userName = userInfo.name

    //def context: SecurityContext
    //val infoProvider: UserInfoProvider
  }

  object Dataporten {
    def createSecurityConnection(accessToken: String) = {
      val infoProvider = new DataportenUserInfoProvider(accessToken)
      val userInfoF = infoProvider.getUserInfo
      val userGroupIdsF = infoProvider.getUserGroupIds

      for {
        //Logger.debug("Før tilordning")
        userInfo <- userInfoF
        userGroupIds <- userGroupIdsF

      } yield new DataportenSecurityConnection(userInfo, userGroupIds)
    }
  }

}