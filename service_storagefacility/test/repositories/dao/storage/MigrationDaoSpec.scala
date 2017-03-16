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

package repositories.dao.storage

import no.uio.musit.models.{ActorId, GroupId, Museums, StorageNodeDatabaseId}
import no.uio.musit.security._
import no.uio.musit.test.MusitSpecWithAppPerSuite
import repositories.dao.MigrationDao

class MigrationDaoSpec extends MusitSpecWithAppPerSuite {

  val migrationDao = fromInstanceCache[MigrationDao]
  val nodeDao      = fromInstanceCache[StorageUnitDao]

  "MigrationDao" should {
    "successfully set STORAGENODE_UUID for nodes that doesn't have one" in {
      implicit val dummyUser = AuthenticatedUser(
        session = UserSession(uuid = SessionUUID.generate()),
        userInfo = UserInfo(
          id = ActorId.generate(),
          secondaryIds = Some(Seq("vader@starwars.com")),
          name = Some("Darth Vader"),
          email = None,
          picture = None
        ),
        groups = Seq(
          GroupInfo(
            id = GroupId.generate(),
            name = "FooBarGroup",
            permission = Permissions.GodMode,
            museumId = Museums.All.id,
            description = None,
            collections = Seq.empty
          )
        )
      )

      val res = migrationDao.generateUUIDWhereEmpty.futureValue

      res.isSuccess mustBe true
      res.get mustBe 10

      for (id <- 7L to 16L) {
        val nid = StorageNodeDatabaseId(id)
        val r   = nodeDao.getById(99, nid).futureValue

        r.isSuccess mustBe true
        r.get.isDefined mustBe true
        r.get.get.nodeId must not be None
      }
    }
  }

}
