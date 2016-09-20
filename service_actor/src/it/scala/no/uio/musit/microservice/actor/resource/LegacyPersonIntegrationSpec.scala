package no.uio.musit.microservice.actor.resource

import no.uio.musit.microservice.actor.domain.Person
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.domain.MusitError
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}

/**
  * Created by sveigl on 20.09.16.
  */
class LegacyPersonIntegrationSpec extends PlaySpec with OneServerPerSuite with ScalaFutures {

  val timeout = PlayTestDefaults.timeout

  override lazy val port: Int = 19007

  implicit override lazy val app = new GuiceApplicationBuilder().configure(PlayTestDefaults.inMemoryDatabaseConfig()).build()


  "LegacyPersonIntegration " must {
    "get by id" in {
      val future = wsUrl("/v1/person/1").get()
      whenReady(future, timeout) { response =>
        val person = Json.parse(response.body).validate[Person].get
        person.id mustBe Some(1)
      }
    }
    "negative get by id" in {
      val future = wsUrl("/v1/person/9999").get()
      whenReady(future, timeout) { response =>
        val error = Json.parse(response.body).validate[MusitError].get
        error.message mustBe "Did not find object with id: 9999"
      }
    }
    "search on person" in {
      val future = wsUrl("/v1/person?search=[And]").get()
      whenReady(future, timeout) { response =>
        val persons = Json.parse(response.body).validate[Seq[Person]].get
        persons.length mustBe 1
        persons.head.fn mustBe "And, Arne1"
      }
    }
    "search on person case insensitive" in {
      val future = wsUrl("/v1/person?search=[and]").get()
      whenReady(future, timeout) { response =>
        val persons = Json.parse(response.body).validate[Seq[Person]].get
        persons.length mustBe 1
        persons.head.fn mustBe "And, Arne1"
      }
    }
    "get root" in {
      val future = wsUrl("/v1/person").get()
      whenReady(future, timeout) { response =>
        val persons = Json.parse(response.body).validate[Seq[Person]].get
        persons.length mustBe 2
      }
    }
    "get person details" in {
      val reqBody: JsValue = Json.parse("[1,2]")
      val future = wsUrl("/v1/person/details").post(reqBody)
      whenReady(future, timeout) { response =>
        val persons = Json.parse(response.body).validate[Seq[Person]].get
        persons.length mustBe 2
        val person1 = persons(0)
        person1.fn mustBe "And, Arne1"
        val person2 = persons(1)
        person2.fn mustBe "Kanin, Kalle1"
      }
    }
    "get person details with extra ids" in {
      val reqBody: JsValue = Json.parse("[1234567,2,9999]")
      val future = wsUrl("/v1/person/details").post(reqBody)
      whenReady(future, timeout) { response =>
        val persons = Json.parse(response.body).validate[Seq[Person]].get
        persons.length mustBe 1
        val person0 = persons(0)
        person0.fn mustBe "Kanin, Kalle1"
      }
    }
  }

}
