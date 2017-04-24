package models.analysis

import java.util.UUID

import no.uio.musit.models.ActorId
import org.scalatest.{Inside, MustMatchers, WordSpec}
import play.api.libs.json.Json

class ActorNameSpec extends WordSpec with MustMatchers with Inside {

  "ActorName" when {

    val testName = "Ola Normann"

    "apply" should {
      "parse actor value to id" in {
        val expectedId = UUID.randomUUID()

        val res = ActorName(expectedId.toString)

        inside(res) {
          case ActorById(id) => id.underlying mustBe expectedId
        }
      }

      "parse actor value to name" in {

        val res = ActorName(testName.toString)

        inside(res) {
          case ActorByName(name) => name mustBe testName
        }
      }
    }

    "Json" should {
      "read name from json" in {
        val jsv = Json.obj(
          "type" -> "ActorByName",
          "name" -> testName
        )

        inside(jsv.as[ActorName]) {
          case ActorByName(name) => name mustBe testName
        }
      }

      "read id from json" in {
        val id = UUID.randomUUID()
        val jsv = Json.obj(
          "type" -> "ActorById",
          "id"   -> id.toString
        )

        inside(jsv.as[ActorName]) {
          case ActorById(aid) => aid.underlying mustBe id
        }
      }

      "write id from ActorById" in {
        val id  = ActorId.generate()
        val res = Json.toJson(ActorById(id))

        res mustBe Json.obj(
          "type" -> "ActorById",
          "id"   -> id.underlying.toString
        )
      }

      "write name from ActorByName" in {
        val id  = ActorId.generate()
        val res = Json.toJson(ActorByName(testName))

        res mustBe Json.obj(
          "type" -> "ActorByName",
          "name" -> testName
        )
      }
    }
  }

}
