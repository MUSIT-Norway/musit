package repositories.auth.dao

import models._
import no.uio.musit.models.{Email, GroupId}
import no.uio.musit.models.Museums.Test
import no.uio.musit.security.{Permissions, StorageFacility}
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
      val grp = GroupAdd(
        s"test$i",
        StorageFacility,
        Permissions.Write,
        Test.id,
        Some(s"test group $i")
      )
      val res = dao.addGroup(grp).futureValue

      addedGroupIds += res.successValue.id
    }
  }

  "Using the AuthDao" when {

    "adding new group data" should {
      "succeed when data is complete" in {
        val grp = GroupAdd(
          "test6",
          StorageFacility,
          Permissions.Read,
          Test.id,
          Some("test group 6")
        )
        val res = dao.addGroup(grp).futureValue

        addedGroupIds += res.successValue.id

        res.successValue.name mustBe "test6"
        res.successValue.permission mustBe Permissions.Read
        res.successValue.description mustBe Some("test group 6")
      }

      "succeed when description isn't set" in {
        val grp = GroupAdd("test7", StorageFacility, Permissions.Write, Test.id, None)
        val res = dao.addGroup(grp).futureValue

        res.isSuccess mustBe true

        addedGroupIds += res.successValue.id

        res.successValue.name mustBe "test7"
        res.successValue.permission mustBe Permissions.Write
        res.successValue.description mustBe None
      }

      "fail if the name is null" in {
        val grp = GroupAdd(
          null,
          StorageFacility,
          Permissions.Read,
          Test.id,
          Some("test group fail")
        ) // scalastyle:ignore
        dao.addGroup(grp).futureValue match {
          case MusitDbError(msg, ex) =>
            msg must include("An error occurred")
            ex must not be None

          case err =>
            fail("Expected MusitDbError")
        }
      }

      "fail if the permission is null" in {
        val grp = GroupAdd(
          "testFail",
          StorageFacility,
          null,
          Test.id,
          Some("test group fail")
        ) // scalastyle:ignore
        dao.addGroup(grp).futureValue match {
          case MusitDbError(msg, ex) =>
            msg must include("An error occurred")
            ex must not be None

          case err =>
            fail("Expected MusitDbError")
        }
      }

      "fail if the module is null" in {
        val grp = GroupAdd(
          "testFail",
          null,
          Permissions.Read,
          Test.id,
          Some("test group fail")
        ) // scalastyle:ignore
        dao.addGroup(grp).futureValue match {
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

  }

}
