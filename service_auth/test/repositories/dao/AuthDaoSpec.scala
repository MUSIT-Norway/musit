/*
 * MUSIT is a museum database to archive natural and cultural history data.
 * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package repositories.dao

import models._
import no.uio.musit.models.{Email, GroupId}
import no.uio.musit.models.Museums.Test
import no.uio.musit.security.Permissions
import no.uio.musit.MusitResults.MusitDbError
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.BeforeAndAfterAll
import org.scalatest.time.{Millis, Seconds, Span}

class AuthDaoSpec extends MusitSpecWithAppPerSuite with BeforeAndAfterAll {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  val dao = fromInstanceCache[AuthDao]

  val addedGroupIds = List.newBuilder[GroupId]

  override def beforeAll(): Unit = {
    // add some groups
    for (i <- 1 to 5) {
      val grp = GroupAdd(s"test$i", Permissions.Write, Test.id, Some(s"test group $i"))
      val res = dao.addGroup(grp).futureValue

      res.isSuccess mustBe true
      addedGroupIds += res.get.id
    }
  }

  "Using the AuthDao" when {

    "adding new group data" should {
      "succeed when data is complete" in {
        val grp = GroupAdd("test6", Permissions.Read, Test.id, Some("test group 6"))
        val res = dao.addGroup(grp).futureValue

        res.isSuccess mustBe true

        addedGroupIds += res.get.id

        res.get.name mustBe "test6"
        res.get.permission mustBe Permissions.Read
        res.get.description mustBe Some("test group 6")
      }

      "succeed when description isn't set" in {
        val grp = GroupAdd("test7", Permissions.Write, Test.id, None)
        val res = dao.addGroup(grp).futureValue

        res.isSuccess mustBe true

        addedGroupIds += res.get.id

        res.get.name mustBe "test7"
        res.get.permission mustBe Permissions.Write
        res.get.description mustBe None
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

        res.isSuccess mustBe true
        res.get must not be None

        val grp = res.get.get
        grp.name mustBe "test1"
        grp.permission mustBe Permissions.Write
        grp.description mustBe Some("test group 1")
      }

      "not return any data when id doesn't exist" in {
        val res = dao.findGroupById(GroupId.generate()).futureValue

        res.isSuccess mustBe true
        res.get mustBe None
      }
    }

    "updating data" should {
      "return the correctly updated data" in {
        val id = addedGroupIds.result().last

        val ug = dao.findGroupById(id).futureValue
        ug.isSuccess mustBe true
        ug.get must not be None

        val updGrp = ug.get.get.copy(
          permission = Permissions.Admin,
          description = Some("test group 7")
        )

        val res = dao.updateGroup(updGrp).futureValue

        res.isSuccess mustBe true
        res.get must not be None

        val grp = res.get.get
        grp.id mustBe id
        grp.name mustBe updGrp.name
        grp.permission mustBe Permissions.Admin
        grp.description mustBe Some("test group 7")
      }

      "fail when setting required field to null" in {
        val id = addedGroupIds.result().last

        val ug = dao.findGroupById(id).futureValue
        ug.isSuccess mustBe true
        ug.get must not be None

        val updGrp = ug.get.get.copy(permission = null) // scalastyle:ignore

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
        val id = addedGroupIds.result().head
        val res = dao.deleteGroup(id).futureValue
        res.isSuccess mustBe true
        res.get mustBe 1
      }

      "not remove any groups if GroupId doesn't exist" in {
        val res = dao.deleteGroup(GroupId.generate()).futureValue
        res.isSuccess mustBe true
        res.get mustBe 0
      }
    }

    "adding a new UserGroup" should {

      val email = Email("foo1@bar.com")

      "successfully add a new UserGroup row" in {
        val grpId = addedGroupIds.result().tail.head
        dao.addUserToGroup(email, grpId, None).futureValue.isSuccess mustBe true
      }

      "not allow duplicate UserGroup entries" in {
        val grpId = addedGroupIds.result().tail.head
        dao.addUserToGroup(email, grpId, None).futureValue.isFailure mustBe true
      }
    }

    "deleting a UserGroup relation" should {
      "successfully remove the row" in {
        val email = Email("foo2@bar.com")
        val gid = addedGroupIds.result().last
        dao.addUserToGroup(email, gid, None).futureValue.isSuccess mustBe true

        val res = dao.removeUserFromGroup(email, gid).futureValue
        res.isSuccess mustBe true
        res.get mustBe 1
      }

      "not remove anything if the userId doesn't exist" in {
        val email = Email("asdf@asdf.net")
        val gid = addedGroupIds.result().last

        val res = dao.removeUserFromGroup(email, gid).futureValue
        res.isSuccess mustBe true
        res.get mustBe 0
      }
    }

    "finding all the groups for a user" should {
      "return all the groups the user is part of" in {
        val email = Email("bar@foo.com")
        val gid1 = addedGroupIds.result().tail.head
        val gid2 = addedGroupIds.result().tail.tail.head
        val gid3 = addedGroupIds.result().last

        dao.addUserToGroup(email, gid1, None).futureValue.isSuccess mustBe true
        dao.addUserToGroup(email, gid2, None).futureValue.isSuccess mustBe true
        dao.addUserToGroup(email, gid3, None).futureValue.isSuccess mustBe true

        val res = dao.findGroupInfoFor(email).futureValue

        res.isSuccess mustBe true
        res.get.size mustBe 3
      }
    }

    "finding all the users in a group" should {
      "return all the ActorIds associated with that group" in {
        val email1 = Email("luke@starwars.com")
        val email2 = Email("leia@starwars.com")
        val email3 = Email("anakin@starwars.com")

        val grp1 = addedGroupIds.result().reverse.tail.tail.head

        dao.addUserToGroup(email1, grp1, None).futureValue.isSuccess mustBe true
        dao.addUserToGroup(email2, grp1, None).futureValue.isSuccess mustBe true
        dao.addUserToGroup(email3, grp1, None).futureValue.isSuccess mustBe true

        val res = dao.findUsersInGroup(grp1).futureValue
        res.isSuccess mustBe true
        res.get.size mustBe 3
        res.get.sortBy(_.value) mustBe Seq(email1, email2, email3).sortBy(_.value)
      }
    }
  }

}
