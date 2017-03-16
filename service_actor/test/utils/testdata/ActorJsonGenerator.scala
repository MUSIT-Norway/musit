package utils.testdata

import play.api.libs.json.{JsValue, Json}

object ActorJsonGenerator {

  def orgJson(
      id: Option[Long],
      name: String,
      nickname: String,
      tel: String,
      web: String
  ): JsValue = {
    Json.parse(
      s"""{
          |  "id" : ${id.orNull},
          |  "fn" : "$name",
          |  "nickname" : "$nickname",
          |  "tel" : "$tel",
          |  "web" : "$web"
          |}
      """.stripMargin
    )
  }

  def orgIllegalJson: JsValue = {
    Json.parse(
      s"""{
          |  "web" : "zzzz"
          |}
      """.stripMargin
    )
  }

  def orgAddressJson = {
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

  def orgAddressIllegalJson = {
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
