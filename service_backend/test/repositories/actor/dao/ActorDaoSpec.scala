package repositories.actor.dao

import java.util.UUID

import models.actor.Person
import no.uio.musit.models.{ActorId, DatabaseId}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues

class ActorDaoSpec extends MusitSpecWithAppPerSuite with MusitResultValues {

  val actorDao: ActorDao = fromInstanceCache[ActorDao]

  val andersAndAppId  = ActorId(UUID.fromString("41ede78c-a6f6-4744-adad-02c25fb1c97c"))
  val kalleKaninAppId = ActorId(UUID.fromString("5224f873-5fe1-44ec-9aaf-b9313db410c6"))

  "ActorDao" when {

    "querying the person legacy methods" should {

      "return None when Id doesn't exist" in {
        actorDao.getByDbId(6386363673636335366L).futureValue mustBe None
      }

      "return a Person if the actor Id is valid" in {
        val expected = Person(
          id = Some(DatabaseId(1L)),
          fn = "And, Arne1",
          dataportenId = None,
          dataportenUser = Some("andarn"),
          applicationId = Some(andersAndAppId)
        )

        actorDao.getByActorId(andersAndAppId).futureValue mustBe Some(expected)
      }

      "return None if the actor Id doesn't exist" in {
        actorDao.getByActorId(ActorId.generate()).futureValue mustBe None
      }

      "return empty list if the search string is not found" in {
        actorDao.getByName(99, "Andlkjlkj").futureValue.isEmpty mustBe true
      }

      "get person details" in {
        val ids     = Set(andersAndAppId, kalleKaninAppId, ActorId.generate())
        val persons = actorDao.listBy(ids).futureValue
        persons.length mustBe 2
        persons.head.fn mustBe "And, Arne1"
        persons.tail.head.fn mustBe "Kanin, Kalle1"
      }

      "not find an actor if the Id from dataporten is unknown" in {
        actorDao
          .getByDataportenId(ActorId(UUID.randomUUID()))
          .futureValue
          .isDefined mustBe false
      }

      "get names for actorId" in {
        val notExisting = ActorId.generate()
        val res = actorDao
          .getNamesForActorIds(Set(andersAndAppId, kalleKaninAppId, notExisting))
          .futureValue
          .successValue

        res must contain(kalleKaninAppId -> "Kanin, Kalle1")
        res must contain(andersAndAppId  -> "And, Arne1")
        res must not contain key(notExisting)
      }
    }
  }
}
