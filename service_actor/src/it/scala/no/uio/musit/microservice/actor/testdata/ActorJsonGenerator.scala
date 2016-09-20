package no.uio.musit.microservice.actor.testdata

import play.api.libs.json.{JsValue, Json}

/**
  * Created by sveigl on 20.09.16.
  */
object ActorJsonGenerator {
  def organisationJson(id: Option[Integer], name:String, nickname: String, tel: String, web: String):JsValue = {
    Json.parse(
      s"""{
          |  "id" : id,
          |  "fn" : "$name",
          |  "nickname" : "$nickname",
          |  "tel" : "$tel",
          |  "web" : "$web"
          |}
      """.stripMargin
    )
  }

}
