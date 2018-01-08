package controllers.conservation

import models.conservation.{MaterialArchaeology, MaterialEthnography, MaterialNumismatic}
import no.uio.musit.models.{MuseumCollections, MuseumId}
import no.uio.musit.security.BearerToken
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import no.uio.musit.test.matchers.DateTimeMatchers
import play.api.libs.json.JsArray
import play.api.test.Helpers._

class ConservationControllerIntegrationSpec
    extends MusitSpecWithServerPerSuite
    with DateTimeMatchers {
  val mid       = MuseumId(99)
  val token     = BearerToken(FakeUsers.testAdminToken)
  val tokenRead = BearerToken(FakeUsers.testReadToken)
  val tokenTest = BearerToken(FakeUsers.testUserToken)

  val baseUrl = (mid: Int) => s"/$mid/conservation"

  val typesUrl           = (mid: Int) => s"${baseUrl(mid)}/types"
  val getRoleListUrl     = s"/conservation/roles"
  val getCondCodeListUrl = s"/conservation/conditionCodes"
  val materialListUrl = (mid: Int, collectionId: String) =>
    s"/$mid/conservation/materials?collectionId=$collectionId"

  val specificMaterialUrl = (id: Int, collectionId: String) =>
    s"/conservation/materials/$id?collectionId=$collectionId"

  def getMaterialList(mid: MuseumId, collectionId: String, t: BearerToken = token) = {
    wsUrl(materialListUrl(mid, collectionId))
      .withHttpHeaders(t.asHeader)
      .get()
      .futureValue
  }

  def getSpecificMaterial(id: Int, collectionId: String, t: BearerToken = token) = {
    wsUrl(specificMaterialUrl(id, collectionId))
      .withHttpHeaders(t.asHeader)
      .get()
      .futureValue
  }

  "Using the conservation controller" when {

    "fetching conservation types" should {

      "return all event types" in {
        val res =
          wsUrl(typesUrl(mid)).withHttpHeaders(tokenRead.asHeader).get().futureValue

        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 10
        (res.json \ 0 \ "noName").as[String] mustBe "konserveringsprosess"
        (res.json \ 0 \ "id").as[Int] mustBe 1
      }
    }
    "fetching role list" should {

      "return the list of roles for actors in conservation events " in {
        val res =
          wsUrl(getRoleListUrl).withHttpHeaders(tokenRead.asHeader).get().futureValue
        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 2
        (res.json \ 0 \ "noRole").as[String] mustBe "Utført av"
        (res.json \ 0 \ "enRole").as[String] mustBe "Done by"
        (res.json \ 0 \ "roleId").as[Int] mustBe 1
        (res.json \ 1 \ "noRole").as[String] mustBe "Deltatt i"
        (res.json \ 1 \ "enRole").as[String] mustBe "Participated in"
        (res.json \ 1 \ "roleId").as[Int] mustBe 2
      }
    }
    "fetching condition code list" should {

      "return the list of codes for using in a conditionAssesment envent " in {
        val res =
          wsUrl(getCondCodeListUrl).withHttpHeaders(tokenRead.asHeader).get().futureValue
        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 4
        (res.json \ 0 \ "noCondition").as[String] mustBe "svært god"
        (res.json \ 0 \ "enCondition").as[String] mustBe "very good"
        (res.json \ 0 \ "conditionCode").as[Int] mustBe 0
        (res.json \ 1 \ "noCondition").as[String] mustBe "god"
        (res.json \ 1 \ "enCondition").as[String] mustBe "good"
        (res.json \ 1 \ "conditionCode").as[Int] mustBe 1
        (res.json \ 2 \ "noCondition").as[String] mustBe "mindre god"
        (res.json \ 2 \ "enCondition").as[String] mustBe "less good"
        (res.json \ 2 \ "conditionCode").as[Int] mustBe 2
        (res.json \ 3 \ "noCondition").as[String] mustBe "dårlig/kritisk"
        (res.json \ 3 \ "enCondition").as[String] mustBe "badly/critical"
        (res.json \ 3 \ "conditionCode").as[Int] mustBe 3
      }
    }
    "working with materialdDetermination and Measurement events " should {
      "get Materiallist for archaeology " in {
        val collection = MuseumCollections.Archeology.uuid.asString
        val res        = getMaterialList(mid, collection)
        res.status mustBe OK
        val list = res.json.validate[Seq[MaterialArchaeology]].get
        list.length mustBe 2
        list.head.noMaterial mustBe "tre"
        list.head.enMaterial mustBe Some("tre[NO]") //there is no english term so we return no_term with NO-tag
      }
      "get Materiallist for ethnography " in {
        val collection = MuseumCollections.Ethnography.uuid.asString
        val res        = getMaterialList(mid, collection)
        res.status mustBe OK
        val list = res.json.validate[Seq[MaterialEthnography]].get
        list.length mustBe 2
        list.head.noMaterial mustBe "silke"
        list.head.noMaterialType mustBe Some("tekstil")
        list.head.enMaterial_type mustBe Some("tekstil[NO]")
        list.head.noMaterial_element mustBe Some("element av ull")
        list.head.enMaterial_element mustBe Some("element av ull[NO]")

        list.tail.head.enMaterial mustBe Some("wool")
        list.tail.head.enMaterial_type mustBe Some("no type")
        list.tail.head.enMaterial_element mustBe Some("element av silke[NO]")
      }
      "get Materiallist for numismatic " in {
        val collection = MuseumCollections.Numismatics.uuid.asString
        val res        = getMaterialList(mid, collection)
        res.status mustBe OK
        val list = res.json.validate[Seq[MaterialNumismatic]].get
        list.length mustBe 2
        list.head.noMaterial mustBe "sølv"
        list.tail.head.enMaterial mustBe Some("gull[NO]") //there is no english term so we return no_term with NO-tag
      }
      "return bad_request when trying to get a Materiallist for naturalhistory " in {
        val collection = MuseumCollections.Fungi.uuid.asString
        val res        = getMaterialList(mid, collection)
        res.status mustBe BAD_REQUEST
      }
      "get materialText from ethnography list with a specific materialId " in {
        val collection      = MuseumCollections.Ethnography.uuid.asString
        val elist           = getMaterialList(mid, collection)
        val lista           = elist.json.validate[Seq[MaterialEthnography]].get
        val materialFirstId = lista.head.materialId
        val materialLastId  = lista.tail.head.materialId
        val res             = getSpecificMaterial(materialFirstId, collection)
        res.status mustBe OK
        val materialFirst = res.json.validate[MaterialEthnography].get
        materialFirst.noMaterial mustBe "silke"
        materialFirst.noMaterialType mustBe Some("tekstil")
        materialFirst.enMaterial_type mustBe Some("tekstil[NO]")
        materialFirst.noMaterial_element mustBe Some("element av ull")
        materialFirst.enMaterial_element mustBe Some("element av ull[NO]")
        val resLast = getSpecificMaterial(materialLastId, collection)
        resLast.status mustBe OK
        val materialLast = resLast.json.validate[MaterialEthnography].get
        materialLast.noMaterial mustBe "ull"
        materialLast.enMaterial mustBe Some("wool")
        materialLast.enMaterial_type mustBe Some("no type")
        materialLast.enMaterial_element mustBe Some("element av silke[NO]")
        materialLast.materialId mustBe 7
      }

      "get materialText from numismatic list with a specific materialId" in {
        val collection      = MuseumCollections.Numismatics.uuid.asString
        val materiallist    = getMaterialList(mid, collection) // no one is hidden in this list
        val list            = materiallist.json.validate[Seq[MaterialNumismatic]].get
        val materialFirstId = list.head.materialId
        val materialLastId  = list.tail.head.materialId
        val res             = getSpecificMaterial(materialFirstId, collection)
        res.status mustBe OK
        val material = res.json.validate[MaterialNumismatic].get
        material.noMaterial mustBe "sølv"
        material.enMaterial mustBe Some("silver")
        material.materialId mustBe 4
        val resLast = getSpecificMaterial(materialLastId, collection)
        resLast.status mustBe OK
        val materialLast = resLast.json.validate[MaterialNumismatic].get
        materialLast.noMaterial mustBe "gull"
        materialLast.enMaterial mustBe Some("gull[NO]")
        materialLast.materialId mustBe 5
      }
      "get materialText from archaeology list with a specific materialId" in {
        val collection   = MuseumCollections.Archeology.uuid.asString
        val materiallist = getMaterialList(mid, collection)
        val list         = materiallist.json.validate[Seq[MaterialArchaeology]].get
        val materialId   = list.head.materialId
        val res          = getSpecificMaterial(materialId, collection)
        res.status mustBe OK
        val material = res.json.validate[MaterialArchaeology].get
        material.noMaterial mustBe "tre"
        material.enMaterial mustBe Some("tre[NO]")
        material.materialId mustBe 1
        val materialLastId = list.tail.head.materialId
        val resLast        = getSpecificMaterial(materialLastId, collection)
        resLast.status mustBe OK
        val materialLast = resLast.json.validate[MaterialNumismatic].get
        materialLast.noMaterial mustBe "jern"
        materialLast.enMaterial mustBe Some("iron")
        materialLast.materialId mustBe 2 // because the last one is hidden in materialList

      }
      "get materialText from archaeology list with a hidden materialId" in {
        val collection = MuseumCollections.Archeology.uuid.asString
        val materialId = 3 // one hidden materialId
        val res        = getSpecificMaterial(materialId, collection)
        res.status mustBe OK
        val material = res.json.validate[MaterialArchaeology]
        material.isSuccess mustBe true
        material.get.noMaterial mustBe "jern"
        material.get.materialId mustBe 3
      }
    }
  }
}
