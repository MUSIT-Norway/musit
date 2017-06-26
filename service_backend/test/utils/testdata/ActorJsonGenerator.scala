package utils.testdata

import play.api.libs.json.{JsValue, Json}

object ActorJsonGenerator {

  def orgJson(
      id: Option[Long],
      fullName: String,
      tel: Option[String],
      web: Option[String],
      synonyms: Option[Seq[String]],
      serviceTags: Option[Seq[String]],
      contact: Option[String],
      email: Option[String]
  ): JsValue = {
    val js1 = Json.obj(
      "fullName" -> fullName,
      "tel"      -> tel,
      "web"      -> web
    )

    // format: off
    val js2 = id.map(i => js1 ++ Json.obj("id" -> i)).getOrElse(js1)
    val js3 = synonyms.map(s => js2 ++ Json.obj("synonyms" -> s)).getOrElse(js2)
    val js4 = serviceTags.map(s => js3 ++ Json.obj("serviceTags" -> s)).getOrElse(js3)
    val js5 = contact.map(c => js4 ++ Json.obj("contact" -> c)).getOrElse(js4)
    email.map(e => js4 ++ Json.obj("email" -> e)).getOrElse(js5)
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
         |  "streetAddress" : "Foo street 2",
         |  "streetAddress2" : "Foo street 3",
         |  "postalCodePlace" : "0342",
         |  "countryName" : "NORWAY"
         |}
       """.stripMargin
    )
  }

  def orgAddressIllegalJson = {
    Json.parse(
      s"""{
         |  "id" : 999,
         |  "organizationId" : 123,
         |  "stretAddres" : "Kirkeveien",
         |  "locality" : "OSLO",
         |  "postalCode" : "0342",
         |  "countryName" : "NORWAY"
         |}
       """.stripMargin
    )
  }

}
