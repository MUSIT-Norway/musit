package no.uio.musit.microservice.storage_admin.resource

import no.uio.musit.microservice.storageAdmin.domain._
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.PlayTestDefaults._
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.extensions.PlayExtensions._
import no.uio.musit.microservices.common.utils.Misc._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsNumber, JsObject, JsString, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by ellenjo on 5/27/16.
 */
class StorageUnitIntegrationSpec extends PlaySpec with OneServerPerSuite with ScalaFutures {
  override lazy val port: Int = 19002
  implicit override lazy val app = new GuiceApplicationBuilder().configure(PlayTestDefaults.inMemoryDatabaseConfig()).build()


  val unknownStorageUnitMsg = (id: Long) => s"Unknown storageUnit with id: $id"

  def createStorageUnit(json: String) = {
    wsUrl("/v1/storageunit").postJsonString(json)
  }

  def updateStorageUnit(id: Long, json: String) = {
    wsUrl(s"/v1/storageunit/$id").putJsonString(json)
  }

  def deleteStorageUnit(id: Long) = {
    wsUrl(s"/v1/storageunit/$id").delete
  }

  def getAll = wsUrl(s"/v1/storageunit").get |> waitFutureValue

  def getPath(id: Long) = wsUrl(s"/v1/storageunit/$id/path").get |> waitFutureValue

  def getStats(id: Long) = wsUrl(s"/v1/storageunit/$id/stats").get |> waitFutureValue

  def getStorageUnit(id: Long) = wsUrl(s"/v1/storageunit/$id").get


  def getRoomAsObject(id: Long): Future[Room] = {
    for {
      resp <- getStorageUnit(id)
      room = Json.parse(resp.body).validate[Storage].get.asInstanceOf[Room]
    } yield room
  }

  def getBuildingAsObject(id: Long): Future[Building] = {
    for {
      resp <- getStorageUnit(id)
      room = Json.parse(resp.body).validate[Storage].get.asInstanceOf[Building]
    } yield room
  }

  def getOrganisationAsObject(id: Long): Future[Organisation] = {
    for {
      resp <- getStorageUnit(id)
      room = Json.parse(resp.body).validate[Storage].get.asInstanceOf[Organisation]
    } yield room
  }


  def getStorageUnitAsObject(id: Long): Future[StorageUnit] = {
    for {
      resp <- getStorageUnit(id)
      stUnit = Json.parse(resp.body).validate[Storage].get.asInstanceOf[StorageUnit]
    } yield stUnit
  }


  val veryLongUnitName =
    """
       12345678901234567890123456789012345678901234567890
       12345678901234567890123456789012345678901234567890
       12345678901234567890123456789012345678901234567890
       12345678901234567890123456789012345678901234567890
       12345678901234567890123456789012345678901234567890
       12345678901234567890123456789012345678901234567890
       12345678901234567890123456789012345678901234567890
       12345678901234567890123456789012345678901234567890
       12345678901234567890123456789012345678901234567890
       12345678901234567890123456789012345678901234567890
       12345678901234567890123456789012345678901234567890
       12345678901234567890123456789012345678901234567890
    """.replace('\n', ' ')


  "StorageUnitIntegration " must {
    "postCreate some IDs" in {
      val makeMyJSon =
        """{"type":"Room","name":"UkjentRom",
        "securityAssessment": {
          "perimeterSecurity": true
        },
        "environmentAssessment": {}

        }""".stripMargin
      val response = createStorageUnit(makeMyJSon) |> waitFutureValue
      response.status mustBe 201
      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[Room]
      storageUnit.id mustBe Some(1)
      storageUnit.name mustBe "UkjentRom"

    }


    "postCreate a room" in {
      val makeMyJSon =
        """
          {
          	"type": "Room",
          	"name": "Trygve Lies rom",
          	"area": 100,
          	"areaTo": 120.25,
          	"height": 2.12,
          	"heightTo": 2.40,
          	"environmentRequirement": {
          		"temperature": 20.4,
          		"temperatureTolerance": 4,
          		"hypoxicAir": 40,
          		"hypoxicAirTolerance": 4,
          		"lightingCondition": "Mørkt",
          		"relativeHumidity": 71,
          		"relativeHumidityTolerance": 4,
          		"cleaning": "Veldig sort",
          		"comments": "Dårlig miljø"
          	},
          	"securityAssessment": {
          		"perimeterSecurity": true,
          		"theftProtection": true,
          		"fireProtection": false,
          		"waterDamage": false,
          	"routinesAndContingencyPlan": true
          	},
          	"environmentAssessment": {
          		"relativeHumidity": true,
          		"temperatureAssessment": true,
          		"lightingCondition": false,
          		"preventiveConservation": true
          	}
          }
        """.stripMargin
      val response = createStorageUnit(makeMyJSon) |> waitFutureValue
      val room = Json.parse(response.body).validate[Storage].get.asInstanceOf[Room]
      room.id mustBe Some(2)
      room.name mustBe "Trygve Lies rom"
      room.area mustBe Some(100.0)
      room.areaTo mustBe Some(120.25)
      room.height mustBe Some(2.12)
      room.heightTo mustBe Some(2.40)
    }

    "get by id" in {
      val response = getStorageUnit(1) |> waitFutureValue
      val storageUnit = Json.parse(response.body).validate[Storage].get
      storageUnit.id mustBe Some(1)
    }
    "negative get by id" in {
      val response = getStorageUnit(9999) |> waitFutureValue
      val error = Json.parse(response.body).validate[MusitError].get
      error.message mustBe unknownStorageUnitMsg(9999)
    }

    "get all nodes" in {
      val response = wsUrl("/v1/storageunit").get() |> waitFutureValue
      val storageUnits = Json.parse(response.body).validate[Seq[Storage]].get
      storageUnits.length mustBe 2

    }

    "update storageUnit" in {
      val myJSon ="""{"type":"StorageUnit","name":"hylle2","areaTo":125}"""
      val response = createStorageUnit(myJSon) |> waitFutureValue
      val storageUnit = Json.parse(response.body).validate[Storage].get
      storageUnit.name mustBe "hylle2"

      storageUnit.id.isDefined mustBe true
      val id = storageUnit.id.get
      val areaTo = storageUnit.areaTo
      areaTo mustBe Some(125)

      val storageJson = Json.parse(response.body).asInstanceOf[JsObject]
        .+("name" -> JsString("hylle3"))
        .+("areaTo" -> JsNumber(130))
        .+("heightTo" -> JsNumber(230))

      val antUpdated = updateStorageUnit(id, storageJson.toString()) |> waitFutureValue
      assert(antUpdated.status == 200)
      val updatedObjectResponse = getStorageUnit(id) |> waitFutureValue
      val updatedObject = Json.parse(updatedObjectResponse.body).validate[Storage].get

      updatedObject.name mustBe "hylle3"
      updatedObject.areaTo mustBe Some(130)
    }

    "update storageRoom" in {
      val myJSon =
        """{"type":"Room","name":"Rom1",
          |         "securityAssessment": {
          |          "perimeterSecurity": false
          |        },
          |        "environmentAssessment": {}
          |
          |
          |
          |}""".stripMargin
      val future = createStorageUnit(myJSon)
      val response = future.futureValue
      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[Room]
      storageUnit.securityAssessment.perimeterSecurity mustBe Some(false)

      storageUnit.id.isDefined mustBe true
      val id = storageUnit.id.get

      storageUnit.name mustBe "Rom1"
      storageUnit.securityAssessment.perimeterSecurity mustBe Some(false)

      val udateRoomJson =
        s"""{"type":"Room","id": $id


, "name":"RomNyttNavn",
           |
            | "securityAssessment": {
            |   "perimeterSecurity": true
            |  },
            | "environmentAssessment": {}
            |
           |}""".stripMargin
      val res = (for {
        _ <- updateStorageUnit(id, udateRoomJson)
        room <- getRoomAsObject(id)
      } yield room) |> waitFutureValue
      res.name mustBe "RomNyttNavn"
      res.securityAssessment.perimeterSecurity mustBe Some(true)

      val myJSonRoom =
        s"""{"type":"Room","id": $id


, "name":"ROM1",
           | "securityAssessment": {
            |   "perimeterSecurity": true
            |  },
            | "environmentAssessment": {}
            |
           |
           |}""".stripMargin

      val future2 = for {
        oppdat <- updateStorageUnit(id, myJSonRoom)
        stUnit <- getRoomAsObject(id)
      } yield stUnit
      val stUnit2 = future2 |> waitFutureValue
      assert(stUnit2.name == "ROM1")
    }

    "update storageBuilding" in {
      val myJSon =
        """{"type":"Building","name":"Bygning0", "address": "vet ikke",
          | "securityAssessment": {
          |   "perimeterSecurity": true
          |  },
          | "environmentAssessment": {}
          |
          |
          |}""".stripMargin
      val future = createStorageUnit(myJSon)
      val response = future |> waitFutureValue
      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[Building]
      storageUnit.address mustBe Some("vet ikke")
      val id = storageUnit.id.get
      storageUnit.name mustBe "Bygning0"
      val udateJson =
        s"""{"type":"Building","id": $id


, "name":"NyBygning", "address": "OrdentligAdresse",
           |
           |        "securityAssessment": {
            |        },
            |        "environmentAssessment": {}

           |}""".stripMargin
      val res = (for {
        res <- updateStorageUnit(id, udateJson)
        room <- getBuildingAsObject(id)
      } yield (room, res)) |> waitFutureValue
      res._1.name mustBe "NyBygning"
      res._1.address mustBe Some("OrdentligAdresse")
    }

    "update room should fail with bad id" in {
      val myJSonRoom =
        """{"type":"Room","name":"ROM1",
          |
          | "securityAssessment": {
          |   "perimeterSecurity": true
          |  },
          | "environmentAssessment": {}
          |
          |}""".stripMargin
      val response = updateStorageUnit(125254764, myJSonRoom) |> waitFutureValue

      val error = Json.parse(response.body).validate[MusitError].get
      error.message mustBe unknownStorageUnitMsg(125254764)
    }

    "postCreate should not be able to insert too long field value" in {
      val makeMyJSon =


      s"""{"type":"Room","name":"$veryLongUnitName",
           |
           |  "securityAssessment": {
            |   "perimeterSecurity": true
            |  },
            | "environmentAssessment": {}
            |
           |
           | }""".stripMargin
      val response = createStorageUnit(makeMyJSon) |> waitFutureValue

      response.status mustBe 500
      response.body must include("Value too long for column")


      //val error = Json.parse(response.body).validate[MusitError].get
      //error.getDeveloperMessage must include("Value too long")
    }


    "create room transaction should not create a storageUnit in the database if the room doesn't get created. (Transaction failure)" in {
      val makeMyJSon =
        """{"type":"Room","name":"UkjentRom2",
          |
          | "securityAssessment": {
          |   "perimeterSecurity": true
          |  },
          | "environmentAssessment": {}
          |}""".stripMargin
      val response = createStorageUnit(makeMyJSon) |> waitFutureValue
      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[Room]

      storageUnit.id.isDefined mustBe true
      val id = storageUnit.id.get //Just to know which is the current id, the next is supposed to fail....

      val jsonWhichShouldFail =
        s"""{"type":"Room","name":"$veryLongUnitName",
            | "securityAssessment": {
            |   "perimeterSecurity": false
            |  },
            | "environmentAssessment": {}
}""".stripMargin
      val response2 = createStorageUnit(jsonWhichShouldFail) |> waitFutureValue

      response2.status mustBe 500
      response2.body must include("Value too long for column")

      val getResponse = getStorageUnit(id + 1) |> waitFutureValue

      val errorOnGet = Json.parse(getResponse.body).validate[MusitError].get
      errorOnGet.message mustBe unknownStorageUnitMsg(id + 1)

    }

    "create and delete room" in {
      val makeMyJSon =
        """{"type":"Room","name":"UkjentRom2",
          |  "securityAssessment": {
          |   "perimeterSecurity": true
          |  },
          | "environmentAssessment": {}
          |}""".stripMargin
      val response = createStorageUnit(makeMyJSon) |> waitFutureValue
      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[Room]
      response.status mustBe 201 //Successfully created the room

      storageUnit.id.isDefined mustBe true
      val id = storageUnit.id.get //Just to know which is the current id, the next is supposed to fail....

      val responsDel = deleteStorageUnit(id) |> waitFutureValue
      responsDel.status mustBe 200 //Successfully deleted the room

      val responsGet = getStorageUnit(id) |> waitFutureValue
      responsGet.status mustBe 404 //Shouldn't find a deleted room

      val responsDel2 = deleteStorageUnit(id) |> waitFutureValue
      responsDel2.status mustBe 404 //Shouldn't be able to delete a deleted room
    }


    "not be able to delete a storageUnit which has never existed" in {
      val responsDel = deleteStorageUnit(12345678) |> waitFutureValue
      responsDel.status mustBe 404
    }

    "not be able to update a deleted storageUnit" in {

      val json =
        """{"type":"StorageUnit","name":"UkjentUnit",
          |
          | "securityAssessment": {
          |   "perimeterSecurity": true
          |  },
          | "environmentAssessment": {}
          |
          |
          |}""".stripMargin
      val response = createStorageUnit(json) |> waitFutureValue
      response.status mustBe 201 //Successfully created the room
      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[StorageUnit]

      storageUnit.id.isDefined mustBe true
      val id = storageUnit.id.get

      val responsDel = deleteStorageUnit(id) |> waitFutureValue
      responsDel.status mustBe 200
      //Successfully deleted

      val updateJson =


        s"""{"type":"StorageUnit","id": $id, "name":"NyUkjentUnit",
           |
           | "securityAssessment": {
            |   "perimeterSecurity": true
            |  },
            | "environmentAssessment": {}
            |}""".stripMargin
      val updateResponse = updateStorageUnit(id, updateJson) |> waitFutureValue
      updateResponse.status mustBe 404 //Should not be able to update a deleted object
    }

    "not be able to update a deleted room" in {

      val json =
        """{"type":"Room","name":"UkjentRom",
          |
          | "securityAssessment": {
          |   "perimeterSecurity": true
          |  },
          | "environmentAssessment": {}
          |}""".stripMargin
      val response = createStorageUnit(json) |> waitFutureValue
      response.status mustBe 201 //Successfully created the room
      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[Room]

      storageUnit.id.isDefined mustBe true
      val id = storageUnit.id.get

      val responsDel = deleteStorageUnit(id) |> waitFutureValue
      responsDel.status mustBe 200 //Successfully deleted

      val updateJson =
        """{"type":"Room","name":"NyttRom",
          |
          | "securityAssessment": {
          |   "perimeterSecurity": true
          |  },
          | "environmentAssessment": {}
          |}""".stripMargin
      val updateResponse = updateStorageUnit(id, updateJson) |> waitFutureValue
      updateResponse.status mustBe 404 //Should not be able to update a deleted object
    }

    "not be able to update a deleted building" in {

      val json =
        """{"type":"Building","name":"UkjentBygning",
          | "securityAssessment": {
          |   "perimeterSecurity": true
          |  },
          | "environmentAssessment": {}
          |}""".stripMargin
      val response = createStorageUnit(json) |> waitFutureValue
      response.status mustBe 201 //Successfully created the room
      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[Building]

      storageUnit.id.isDefined mustBe true
      val id = storageUnit.id.get

      val responsDel = deleteStorageUnit(id) |> waitFutureValue
      responsDel.status mustBe 200 //Successfully deleted

      val updateJson =
        """{"type":"Building","name":"NyBygning", "address": "OrdentligAdresse",
          | "securityAssessment": {
          |   "perimeterSecurity": true
          |  },
          | "environmentAssessment": {}
          |
          |}""".stripMargin
      val updateResponse = updateStorageUnit(id, updateJson) |> waitFutureValue
      updateResponse.status mustBe 404 //Should not be able to update a deleted object
    }


    "update should fail (with Conflict=409) if inconsistent storage types" in {
      val json =
        """{"type":"Room","name":"UkjentRom2",
          | "securityAssessment": {
          |   "perimeterSecurity": true
          |  },
          | "environmentAssessment": {}
          |
          |
          |}""".stripMargin
      val response = createStorageUnit(json) |> waitFutureValue
      response.status mustBe 201 //Created

      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[Room]

      storageUnit.id.isDefined mustBe true
      val id = storageUnit.id.get

      val updatedJson =
        """{"type":"Building", "name":"Ukjent bygning", "address":"HelloAddress",
          | "securityAssessment": {
          |   "perimeterSecurity": true
          |  },
          | "environmentAssessment": {}
          |
          |}""".stripMargin
      val responseUpdate = updateStorageUnit(id, updatedJson) |> waitFutureValue
      responseUpdate.status mustBe 409 //Conflict
    }
    "create should fail with invalid input data" in {
      val
      json =
        """{"type":"Room","name":"UkjentRom2", "securityAssessment": {
                  |   "perimeterSecurity": 1
                  |  },
                  | "environmentAssessment": {}
                  |}""".stripMargin
      val response = createStorageUnit(json) |> waitFutureValue
      response.status mustBe 400
    }

    "getChildren should return correct type" in {

      val makeMyJSonBase ="""{"type":"Building","name":"KHM0", "links":[]}"""
      val responseBase = createStorageUnit(makeMyJSonBase) |> waitFutureValue
      val storageUnitBase = Json.parse(responseBase.body).validate[Storage].get.asInstanceOf[Building]
      val byggIdBase = storageUnitBase.id.get
      storageUnitBase.name mustBe "KHM0"

      val makeMyJSonChild1 = s"""{"type":"Building", "isPartOf": $byggIdBase, "name":"KHM1", "links":[]}"""
      val responseChild1 = createStorageUnit(makeMyJSonChild1) |> waitFutureValue
      val storageUnitChild1 = Json.parse(responseChild1.body).validate[Storage].get.asInstanceOf[Building]
      storageUnitChild1.id mustBe Some(byggIdBase+1)
      storageUnitChild1.name mustBe "KHM1"


      val makeMyJSonChild2 =
        s"""{
            |          	"type": "Room",
            |           "isPartOf": $byggIdBase,
            |          	"name": "Trygve Lies rom 2",
            |          	"area": 100,
            |          	"areaTo": 120.25,
            |          	"height": 2.12,
            |          	"heightTo": 2.40,
            |          	"environmentRequirement": {
            |          		"temperature": 20.4,
            |          		"temperatureTolerance": 4,
            |          		"hypoxicAir": 40,
            |          		"hypoxicAirTolerance": 4,
            |          		"lightingCondition": "Mørkt",
            |          		"relativeHumidity": 71,
            |          		"relativeHumidityTolerance": 4,
            |          		"cleaning": "Veldig sort",
            |          		"comments": "Dårlig miljø"
            |          	},
            |          	"securityAssessment": {
            |          		"perimeter": true,
            |          		"theftProtection": true,
            |          		"fireProtection": false,
            |          		"waterDamage": false,
            |          	"routinesAndContingencyPlan": true
            |          	},
            |          	"environmentAssessment": {
            |          		"relativeHumidity": true,
            |          		"temperatureAssessment": true,
            |          		"lightingCondition": false,
            |          		"preventiveConservation": true
            |          	}
            |          }""".stripMargin
      val responseChild2 = createStorageUnit(makeMyJSonChild2) |> waitFutureValue
      val storageUnitChild2 = Json.parse(responseChild2.body).validate[Storage].get.asInstanceOf[Room]
      storageUnitChild2.id mustBe Some(byggIdBase+2)
      storageUnitChild2.name mustBe "Trygve Lies rom 2"

      val responseChildren = wsUrl(s"/v1/storageunit/$byggIdBase/children").get() |> waitFutureValue
      responseChildren.status mustBe 200
      val storageUnits = Json.parse(responseChildren.body).validate[Seq[Storage]].get
      storageUnits.length mustBe 2
      val su1 = storageUnits(0).asInstanceOf[Building]
      val su2 = storageUnits(1).asInstanceOf[Room]



    }

    "postCreate a organisation" in {
      val makeMyJSon ="""{"id":-1, "type":"Organisation","name":"KHM", "links":[]}"""
      val response = createStorageUnit(makeMyJSon) |> waitFutureValue
      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[Organisation]
      storageUnit.name mustBe "KHM"

    }

    "update storageOrganisation" in {
      val myJSon ="""{"type":"Organisation","name":"Organisasjon0", "address": "vet ikke"}"""
      val future = createStorageUnit(myJSon)
      val response = future |> waitFutureValue
      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[Organisation]
      storageUnit.address mustBe Some("vet ikke")
      val id = storageUnit.id.get
      storageUnit.name mustBe "Organisasjon0"
      val udateJson = s"""{"type":"Organisation","id": $id, "name":"NyOrganisasjon", "address": "OrdentligAdresse"}"""
      val res = (for {
        res <- updateStorageUnit(id, udateJson)
        room <- getOrganisationAsObject(id)
      } yield (room, res)) |> waitFutureValue
      res._1.name mustBe "NyOrganisasjon"
      res._1.address mustBe Some("OrdentligAdresse")
    }
    "not be able to update a deleted organisation" in {

      val json ="""{"type":"Organisation","name":"UkjentOrganisasjon"}"""
      val response = createStorageUnit(json) |> waitFutureValue
      response.status mustBe 201 //Successfully created the room
      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[Organisation]

      storageUnit.id.isDefined mustBe true
      val id = storageUnit.id.get

      val responsDel = deleteStorageUnit(id) |> waitFutureValue
      responsDel.status mustBe 200 //Successfully deleted

      val updateJson = """{"type":"Organisation","name":"NyOrganisasjon", "address": "OrdentligAdresse"}"""
      val updateResponse = updateStorageUnit(id, updateJson) |> waitFutureValue
      updateResponse.status mustBe 404 //Should not be able to update a deleted object
    }



    "get all nodes should return different kinds of nodes and their specific properties" in {
      val response = getAll
      response.status mustBe 200
      response.body must include("perimeterSecurity")

      response

        .body must include("address")
      response.body must
        include(
        "UkjentRom2")

    }

        "create room with envReq" in {
        val makeMyJSon =
          """{"type":"Room","name":"UkjentRom2",
            |  "securityAssessment": {
            |   "perimeterSecurity": true
            |  },
            | "environmentAssessment": {},
            |  "environmentRequirement": {
            |     "temperature": 20.4,
            |     "temperatureTolerance": 4,
            |     "hypoxicAir": 40,
            |     "hypoxicAirTolerance": 4,
            |     "lightingCondition": "Mørkt",
            |     "relativeHumidity": 71,
            |     "relativeHumidityTolerance": 4,
            |     "cleaning": "Veldig sort",
            |     "comments": "Dårlig miljø"
            |   }
            |}""".
            stripMargin
        val response
        = createStorageUnit(makeMyJSon) |> waitFutureValue

          val storageUnit = Json.parse(response.body).
            validate[Storage].get.
            asInstanceOf[Room]
        response.status mustBe 201
          //Successfully created the room

        storageUnit.id.isDefined mustBe true
          val id = storageUnit.id.get

          //Just to know which is the current id, the next is supposed to fail....
        val responsGet =
        getStorageUnit(id) |> waitFutureValue
       val room = Json.
         parse(responsGet.body).validate[Storage
         ].get.asInstanceOf[Room]

        val
        optEnvReq = room.environmentRequirement
          assert(optEnvReq.isDefined)

        val
        envReq = optEnvReq.get
        envReq.
          temperature mustBe Some(20.4)
        envReq.
          temperatureTolerance mustBe Some(4)
          envReq.hypoxicAir mustBe Some(40)
          envReq.
            hypoxicAirTolerance mustBe Some(4)
          envReq.lightingCondition mustBe Some("Mørkt")
          envReq.relativeHumidity mustBe Some(71)
          envReq.relativeHumidityTolerance mustBe Some(4)
          envReq.cleaning mustBe Some("Veldig sort")
          envReq.comments mustBe Some("Dårlig miljø")
        }


    "create building with envReq" in {
      val makeMyJSon =
        """{"type":"Building","name":"UkjentBygning",
          |
          |  "environmentRequirement": {
          |     "temperature": 20.4,
          |     "temperatureTolerance": 4,
          |     "hypoxicAir": 40,
          |     "hypoxicAirTolerance": 4,
          |     "lightingCondition": "Mørkt",
          |     "relativeHumidity": 71,
          |     "relativeHumidityTolerance": 4,
          |     "cleaning": "Veldig sort",
          |     "comments": "Dårlig miljø"
          |   }
          |}""".
          stripMargin
      val response = createStorageUnit(makeMyJSon) |> waitFutureValue
      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[Building]
      response.status mustBe 201 //Successfully created the room

      storageUnit.id.isDefined mustBe true
      val id = storageUnit.id.get
      //Just to know which is the current id, the next is supposed to fail....
      val responsGet = getStorageUnit(id) |> waitFutureValue
      val building = Json.parse(responsGet.body).validate[Storage].get.asInstanceOf[Building]

      val optEnvReq = building.environmentRequirement
      assert(optEnvReq.isDefined)

      val envReq = optEnvReq.get
      envReq.temperature mustBe Some(20.4)
      envReq.temperatureTolerance mustBe Some(4)
      envReq.hypoxicAir mustBe Some(40)
      envReq.hypoxicAirTolerance mustBe Some(4)
      envReq.lightingCondition mustBe Some("Mørkt")
      envReq.relativeHumidity mustBe Some(71)
      envReq.relativeHumidityTolerance mustBe Some(4)
      envReq.cleaning mustBe Some("Veldig sort")
      envReq.comments mustBe Some("Dårlig miljø")
    }



    "create storage unit with envReq" in {
      val makeMyJSon =
        """{"type":"StorageUnit","name":"Ukjent storage unit",
          |
          |  "environmentRequirement": {
          |     "temperature": 20.4,
          |     "temperatureTolerance": 4,
          |     "hypoxicAir": 40,
          |     "hypoxicAirTolerance": 4,
          |     "lightingCondition": "Mørkt",
          |     "relativeHumidity": 71,
          |     "relativeHumidityTolerance": 4,
          |     "cleaning": "Veldig sort",
          |     "comments": "Dårlig miljø"
          |   }
          |}""".
          stripMargin
      val response = createStorageUnit(makeMyJSon) |> waitFutureValue
      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[StorageUnit]
      response.status mustBe 201 //Successfully created the room

      storageUnit.id.isDefined mustBe true
      val id = storageUnit.id.get
      //Just to know which is the current id, the next is supposed to fail....
      val responsGet = getStorageUnit(id) |>
        waitFutureValue
      val stUnit = Json.parse(responsGet.body).validate[Storage].get.asInstanceOf[StorageUnit]

      val optEnvReq = stUnit.environmentRequirement
      assert(optEnvReq.isDefined)

      val envReq = optEnvReq.get
      envReq.temperature mustBe Some(20.4)
      envReq.temperatureTolerance mustBe Some(4)
      envReq.hypoxicAir mustBe Some(40)
      envReq.hypoxicAirTolerance mustBe Some(4)
      envReq.lightingCondition mustBe Some("Mørkt")
      envReq.relativeHumidity mustBe Some(71)
      envReq.relativeHumidityTolerance mustBe Some(4)
      envReq.cleaning mustBe Some("Veldig sort")
      envReq.comments mustBe Some("Dårlig miljø")
    }



    "update storage unit with envReq" in {
      val makeMyJSon =
        """{"type":"StorageUnit","name":"Ukjent storage unit",
          |
          |  "environmentRequirement": {
          |     "temperature": 20.4,
          |     "temperatureTolerance": 4,
          |     "hypoxicAir": 40,
          |     "hypoxicAirTolerance": 4,
          |     "lightingCondition": "Mørkt",
          |     "relativeHumidity": 71,
          |     "relativeHumidityTolerance": 4,
          |     "cleaning": "Veldig sort",
          |     "comments": "Dårlig miljø"
          |   }
          |}""".
          stripMargin
      val response = createStorageUnit(makeMyJSon) |> waitFutureValue
      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[StorageUnit]
      response.status mustBe 201 //Successfully created the room

      storageUnit.name mustBe "Ukjent storage unit"
      storageUnit.id.isDefined mustBe true
      val id = storageUnit.id.get


      val updateJson =
        """{"type":"StorageUnit","name":"Nytt navn",
          |
          |  "environmentRequirement": {
          |     "temperature": 21,
          |     "hypoxicAir": 40,
          |     "hypoxicAirTolerance": 4,
          |     "lightingCondition": "Mørkt",
          |     "relativeHumidity": 71,
          |     "relativeHumidityTolerance": 4,
          |     "cleaning": "Veldig sort",
          |     "comments": "Bra miljø"
          |   }
          |}""".
          stripMargin


      val responsUpdate = updateStorageUnit(id, updateJson) |> waitFutureValue
      val stUnit = Json.parse(responsUpdate.body).validate[Storage].get.asInstanceOf[StorageUnit]

      stUnit.name mustBe "Nytt navn"

      val optEnvReq = stUnit.environmentRequirement
      assert(optEnvReq.isDefined)

      val envReq = optEnvReq.get
      envReq.temperature mustBe Some(21)
      envReq.temperatureTolerance mustBe None
      envReq.hypoxicAir mustBe Some(40)
      envReq.hypoxicAirTolerance mustBe Some(4)
      envReq.lightingCondition mustBe Some("Mørkt")
      envReq.relativeHumidity mustBe Some(71)
      envReq.relativeHumidityTolerance mustBe Some(4)
      envReq.cleaning mustBe Some("Veldig sort")
      envReq.comments mustBe Some("Bra miljø")


      //Also check that getPath returns something. This ought to be a separate test, but we are lazy and retrieves an
      //already inserted storageUnit

      val pathRes = getPath(id)
      pathRes.status mustBe 200

      pathRes.body must include("Nytt navn")
      pathRes.body must include(id.toString)
      pathRes.body must include("StorageUnit")
    }

    "test Stats" in {
      val makeMyJSon =
        """{"type":"Room","name":"UkjentRom",
        "securityAssessment": {
          "perimeterSecurity": true
        },
        "environmentAssessment": {}
        }"""
      val response = createStorageUnit(makeMyJSon) |> waitFutureValue
      response.status mustBe 201
      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[Room]


      val subNodeJson =
        s"""{"type":"StorageUnit","name":"UkjentBoks",
           "isPartOf":${storageUnit.id.get}
        }"""

      val response2 = createStorageUnit(subNodeJson) |> waitFutureValue
      response2.status mustBe 201
      val storageUnit2 = Json.parse(response2.body).validate[Storage].get.asInstanceOf[StorageUnit]



      val statsResponse = getStats(storageUnit.id.get)
      statsResponse.status mustBe 200
      val stats = Json.parse(statsResponse.body).validate[Stats].get

      //TODO: We don't have any mechanism from the REST API yet to build up object structure in this test,
      // so rather boring numbers here...

      stats.nodes mustBe 1
      stats.objects mustBe 0
      stats.totalObjects mustBe 0

      val stats2Response = getStats(storageUnit2.id.get)
      statsResponse.status mustBe 200
      val stats2 = Json.parse(stats2Response.body).validate[Stats].get
      stats2.nodes mustBe 0
      stats2.objects mustBe 0
      stats2.totalObjects mustBe 0
    }
  }
}