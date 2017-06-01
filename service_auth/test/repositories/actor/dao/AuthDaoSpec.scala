package repositories.actor.dao

import models._
import no.uio.musit.models.{Email, GroupId}
import no.uio.musit.models.Museums.Test
import no.uio.musit.security.Permissions
import no.uio.musit.MusitResults.MusitDbError
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import org.scalatest.BeforeAndAfterAll

class AuthDaoSpec
    extends MusitSpecWithAppPerSuite
    with BeforeAndAfterAll
    with MusitResultValues {

  val dao = fromInstanceCache[AuthDao]

  val addedGroupIds = List.newBuilder[GroupId]

  override def beforeAll(): Unit = {
    // add some groups
    for (i <- 1 to 5) {
      val grp = GroupAdd(s"test$i", Permissions.Write, Test.id, Some(s"test group $i"))
      val res = dao.addGroup(grp).futureValue

      addedGroupIds += res.successValue.id
    }
  }

  "Using the AuthDao" when {

    "adding new group data" should {
      "succeed when data is complete" in {
        val grp = GroupAdd("test6", Permissions.Read, Test.id, Some("test group 6"))
        val res = dao.addGroup(grp).futureValue

        addedGroupIds += res.successValue.id

        res.successValue.name mustBe "test6"
        res.successValue.permission mustBe Permissions.Read
        res.successValue.description mustBe Some("test group 6")
      }

      "succeed when description isn't set" in {
        val grp = GroupAdd("test7", Permissions.Write, Test.id, None)
        val res = dao.addGroup(grp).futureValue

        res.isSuccess mustBe true

        addedGroupIds += res.successValue.id

        res.successValue.name mustBe "test7"
        res.successValue.permission mustBe Permissions.Write
        res.successValue.description mustBe None
      }

      "fail if the name is null" in {
        val grp = GroupAdd(null, Permissions.Read, Test.id, Some("test group fail")) // scalastyle:ignore
        dao.addGroup(grp).futureValue match {
          case MusitDbError(msg, ex) =>
            msg must include("An error occurred")
            ex must not be None

          case err =>
            fail("Expected MusitDbError")
        }
      }

      "fail if the permission is null" in {
        val grp = GroupAdd("testFail", null, Test.id, Some("test group fail")) // scalastyle:ignore
        dao.addGroup(grp).futureValue match {
          case MusitDbError(msg, ex) =>
            msg must include("An error occurred")
            ex must not be None

          case err =>
            fail("Expected MusitDbError")
        }
      }
    }

    "finding data by GroupId" should {
      "return the correct data when it exists" in {
        val id = addedGroupIds.result().head

        val res = dao.findGroupById(id).futureValue

        val grp = res.successValue.value
        grp.name mustBe "test1"
        grp.permission mustBe Permissions.Write
        grp.description mustBe Some("test group 1")
      }

      "not return any data when id doesn't exist" in {
        val res = dao.findGroupById(GroupId.generate()).futureValue

        res.successValue mustBe None
      }
    }

    "updating data" should {
      "return the correctly updated data" in {
        val id = addedGroupIds.result().last

        val ug = dao.findGroupById(id).futureValue

        val updGrp = ug.successValue.value.copy(
          permission = Permissions.Admin,
          description = Some("test group 7")
        )

        val res = dao.updateGroup(updGrp).futureValue

        val grp = res.successValue.value
        grp.id mustBe id
        grp.name mustBe updGrp.name
        grp.permission mustBe Permissions.Admin
        grp.description mustBe Some("test group 7")
      }

      "fail when setting required field to null" in {
        val id = addedGroupIds.result().last

        val ug = dao.findGroupById(id).futureValue

        val updGrp = ug.successValue.value.copy(permission = null) // scalastyle:ignore

        dao.updateGroup(updGrp).futureValue match {
          case MusitDbError(msg, ex) =>
            msg must include("An error occurred")
            ex must not be None

          case err =>
            fail("Expected MusitDbError")
        }
      }
    }

    "deleting a group" should {
      "successfully remove a group" in {
        val id  = addedGroupIds.result().head
        val res = dao.deleteGroup(id).futureValue
        res.successValue mustBe 1
      }

      "not remove any groups if GroupId doesn't exist" in {
        val res = dao.deleteGroup(GroupId.generate()).futureValue
        res.successValue mustBe 0
      }
    }

    "adding a new UserGroup" should {

      val email = Email("foo1@bar.com")

      "successfully add a new UserGroup row" in {
        val grpId = addedGroupIds.result().tail.head
        dao.addUserToGroup(email, grpId, None).futureValue.successValue
      }

      "not allow duplicate UserGroup entries" in {
        val grpId = addedGroupIds.result().tail.head
        dao.addUserToGroup(email, grpId, None).futureValue.isFailure mustBe true
      }
    }

    "deleting a UserGroup relation" should {
      "successfully remove the row" in {
        val email = Email("foo2@bar.com")
        val gid   = addedGroupIds.result().last
        dao.addUserToGroup(email, gid, None).futureValue.isSuccess mustBe true

        val res = dao.removeUserFromGroup(email, gid).futureValue
        res.successValue mustBe 1
      }

      "not remove anything if the userId doesn't exist" in {
        val email = Email("asdf@asdf.net")
        val gid   = addedGroupIds.result().last

        val res = dao.removeUserFromGroup(email, gid).futureValue
        res.successValue mustBe 0
      }
    }

    "finding all the groups for a user" should {
      "return all the groups the user is part of" in {
        val email = Email("bar@foo.com")
        val gid1  = addedGroupIds.result().tail.head
        val gid2  = addedGroupIds.result().tail.tail.head
        val gid3  = addedGroupIds.result().last

        dao.addUserToGroup(email, gid1, None).futureValue.successValue
        dao.addUserToGroup(email, gid2, None).futureValue.successValue
        dao.addUserToGroup(email, gid3, None).futureValue.successValue

        val res = dao.findGroupInfoFor(email).futureValue

        res.successValue.size mustBe 3
      }

      "return all the groups for a user stored with UPPER_CASE email" in {
        val email = Email("foobar@baz.com")

        val res = dao.findGroupInfoFor(email).futureValue

        res.successValue.size mustBe 1
      }
    }

    "finding all the users in a group" should {
      "return all the ActorIds associated with that group" in {
        val email1 = Email("luke@starwars.com")
        val email2 = Email("leia@starwars.com")
        val email3 = Email("anakin@starwars.com")

        val grp1 = addedGroupIds.result().reverse.tail.tail.head

        dao.addUserToGroup(email1, grp1, None).futureValue.successValue
        dao.addUserToGroup(email2, grp1, None).futureValue.successValue
        dao.addUserToGroup(email3, grp1, None).futureValue.successValue

        val res = dao.findUsersInGroup(grp1).futureValue
        res.successValue.size mustBe 3
        res.successValue.sortBy(_.value) mustBe
          Seq(email1, email2, email3).sortBy(_.value)
      }
    }
  }

}
