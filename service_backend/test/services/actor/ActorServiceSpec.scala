package services.actor

import models.actor.Person
import no.uio.musit.models.{ActorId, DatabaseId, Email}
import no.uio.musit.security.UserInfo
import no.uio.musit.test.MusitSpecWithAppPerSuite

class ActorServiceSpec extends MusitSpecWithAppPerSuite {

  val service = fromInstanceCache[ActorService]

  def generatePersonSeq: Seq[Person] = {
    (0 until 15).map { i =>
      Person(
        id = DatabaseId.fromOptLong(Some(i.toLong)),
        fn = s"Full $i Name",
        dataportenUser = Some(s"${i}user"),
        applicationId = ActorId.generateAsOpt()
      )
    }
  }

  def generateUserInfos: Seq[UserInfo] = {
    Seq(4, 7, 11).map { i =>
      UserInfo(
        id = ActorId.generate(),
        secondaryIds = Some(Seq(s"${i}user@foo.io")),
        name = Some(s"Full $i Name"),
        email = Some(Email(s"user$i@bar.foo.io")),
        picture = None
      )
    }
  }

  "The ActorService" should {

    "merge duplicate entries in a Person list into a list of UserInfo" in {
      val persons = generatePersonSeq
      val users   = generateUserInfos

      val res = service.merge(users, persons).sortBy { p =>
        // isolate the number part of the ID
        p.dataportenUser.map(_.stripSuffix("@foo.io").stripSuffix("user").toInt)
      }

      res.size mustBe 15

      res(4).applicationId mustBe persons(4).applicationId
      res(7).applicationId mustBe persons(7).applicationId
      res(11).applicationId mustBe persons(11).applicationId
      res(4).dataportenId mustBe users.headOption.map(_.id)
      res(7).dataportenId mustBe users.tail.headOption.map(_.id)
      res(11).dataportenId mustBe users.lastOption.map(_.id)
    }

  }

}
