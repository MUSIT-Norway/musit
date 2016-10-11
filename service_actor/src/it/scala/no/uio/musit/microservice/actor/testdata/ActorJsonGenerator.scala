package no.uio.musit.microservice.actor.testdata

import play.api.libs.json.{JsValue, Json}

object ActorJsonGenerator {
  def organisationJson(id: Option[Long], name: String, nickname: String, tel: String, web: String): JsValue = {
    Json.parse(
      s"""{
          |  "id" : ${id.getOrElse(null)},
          |  "fn" : "$name",
          |  "nickname" : "$nickname",
          |  "tel" : "$tel",
          |  "web" : "$web"
          |}
      """.stripMargin
    )
  }
  def organisationIllegalJson: JsValue = {
    Json.parse(
      s"""{
          |  "web" : "zzzz"
          |}
      """.stripMargin
    )
  }

  def organizationAddressJson = {
    Json.parse(
      s"""{
          |  "organizationId" : 1,
          |  "addressType" : "TEST",
          |  "streetAddress" : "Foo street 2",
          |  "locality" : "OSLO",
          |  "postalCode" : "0342",
          |  "countryName" : "NORWAY",
          |  "latitude" : 60,
          |  "longitude" : 11.05
          |}
       """.stripMargin
    )
  }
  def organizationAddressIllegalJson = {
    Json.parse(
      s"""{
          |  "id" : 999,
          |  "organizationId" : 123,
          |  "adressType" : "WORK",
          |  "stretAddres" : "Kirkeveien",
          |  "locality" : "OSLO",
          |  "postalCode" : "0342",
          |  "countryName" : "NORWAY",
          |  "latitude" : 60,
          |  "longitude" : 11.05
          |}
       """.stripMargin
    )
  }

}
