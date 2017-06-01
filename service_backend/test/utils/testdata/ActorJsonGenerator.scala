package utils.testdata

import play.api.libs.json.{JsValue, Json}

object ActorJsonGenerator {

  def orgJson(
      id: Option[Long],
      fullName: String,
      tel: Option[String],
      web: Option[String],
      synonyms: Option[Seq[String]],
      serviceTags: Option[Seq[String]]
  ): JsValue = {
    val js1 = Json.obj(
      "fullName" -> fullName,
      "tel"      -> tel,
      "web"      -> web
    )

    // format: off
    val js2 = id.map(i => js1 ++ Json.obj("id" -> i)).getOrElse(js1)
    val js3 = synonyms.map(s => js2 ++ Json.obj("synonyms" -> s)).getOrElse(js2)
    serviceTags.map(s => js3 ++ Json.obj("serviceTags" -> s)).getOrElse(js3)
    // format: on
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
