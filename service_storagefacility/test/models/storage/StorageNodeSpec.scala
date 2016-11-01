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

package models.storage

import models.storage.StorageType._
import no.uio.musit.models.{NodePath, StorageNodeId}
import org.scalatest.{MustMatchers, WordSpec}

class StorageNodeSpec extends WordSpec with MustMatchers {

  "A Root node" should {

    "be allowed at the top location in a node hierarchy" in {
      Root.isValidLocation(NodePath.empty) mustBe true
    }

    "not be valid other than top location in a node hierarchy" in {
      Root.isValidLocation(NodePath(",1,2,3,")) mustBe false
    }

  }

  "An Organisation node" should {

    "not be allowed to be placed when there's no destination Id" in {
      Organisation.isValidLocation(
        maybeDestId = None,
        pathTypes = Seq(StorageNodeId(1) -> RootType)
      ) mustBe false
    }

    "be allowed directly under a root node" in {
      Organisation.isValidLocation(
        maybeDestId = Some(StorageNodeId(1)),
        pathTypes = Seq(StorageNodeId(1) -> RootType)
      ) mustBe true
    }

    "not be allowed under a top-level organisation node" in {
      Organisation.isValidLocation(
        maybeDestId = Some(StorageNodeId(2)),
        pathTypes = Seq(
          StorageNodeId(1) -> RootType,
          StorageNodeId(2) -> OrganisationType
        )
      ) mustBe false
    }

    "be allowed under a top-level building node" in {
      Organisation.isValidLocation(
        maybeDestId = Some(StorageNodeId(3)),
        pathTypes = Seq(
          StorageNodeId(1) -> RootType,
          StorageNodeId(2) -> OrganisationType,
          StorageNodeId(3) -> BuildingType
        )
      ) mustBe true
    }

    "be allowed anywhere after the 3 required top-nodes" in {
      Organisation.isValidLocation(
        maybeDestId = Some(StorageNodeId(5)),
        pathTypes = Seq(
          StorageNodeId(1) -> RootType,
          StorageNodeId(2) -> OrganisationType,
          StorageNodeId(3) -> BuildingType,
          StorageNodeId(4) -> RoomType,
          StorageNodeId(5) -> StorageUnitType
        )
      ) mustBe true
    }

  }

  "A Building node" should {

    "not be allowed directly under a root node" in {
      Building.isValidLocation(
        maybeDestId = Some(StorageNodeId(1)),
        pathTypes = Seq(StorageNodeId(1) -> RootType)
      ) mustBe false
    }

    "be allowed under a top-level organisation node" in {
      Building.isValidLocation(
        maybeDestId = Some(StorageNodeId(2)),
        pathTypes = Seq(
          StorageNodeId(1) -> RootType,
          StorageNodeId(2) -> OrganisationType
        )
      ) mustBe true
    }

    "not be allowed to be placed when there's no destination Id" in {
      Building.isValidLocation(
        maybeDestId = None,
        pathTypes = Seq(
          StorageNodeId(1) -> RootType,
          StorageNodeId(2) -> OrganisationType
        )
      ) mustBe false
    }

    "be allowed under a top-level building node" in {
      Building.isValidLocation(
        maybeDestId = Some(StorageNodeId(3)),
        pathTypes = Seq(
          StorageNodeId(1) -> RootType,
          StorageNodeId(2) -> OrganisationType,
          StorageNodeId(3) -> BuildingType
        )
      ) mustBe true
    }

    "be allowed anywhere after the 3 required top-nodes" in {
      Building.isValidLocation(
        maybeDestId = Some(StorageNodeId(5)),
        pathTypes = Seq(
          StorageNodeId(1) -> RootType,
          StorageNodeId(2) -> OrganisationType,
          StorageNodeId(3) -> BuildingType,
          StorageNodeId(4) -> RoomType,
          StorageNodeId(5) -> StorageUnitType
        )
      ) mustBe true
    }
  }

  "A Room node" should {

    "not be allowed under a root node" in {
      Room.isValidLocation(
        maybeDestId = Some(StorageNodeId(1)),
        pathTypes = Seq(StorageNodeId(1) -> RootType)
      ) mustBe false
    }

    "not be allowed under a top-level organisation node" in {
      Room.isValidLocation(
        maybeDestId = Some(StorageNodeId(2)),
        pathTypes = Seq(
          StorageNodeId(1) -> RootType,
          StorageNodeId(2) -> OrganisationType
        )
      ) mustBe false
    }

    "be allowed under a top-level building node" in {
      Room.isValidLocation(
        maybeDestId = Some(StorageNodeId(3)),
        pathTypes = Seq(
          StorageNodeId(1) -> RootType,
          StorageNodeId(2) -> OrganisationType,
          StorageNodeId(3) -> BuildingType
        )
      ) mustBe true
    }

    "not be allowed to be placed when there's no destination Id" in {
      Room.isValidLocation(
        maybeDestId = None,
        pathTypes = Seq(
          StorageNodeId(1) -> RootType,
          StorageNodeId(2) -> OrganisationType,
          StorageNodeId(3) -> BuildingType
        )
      ) mustBe false
    }

    "be allowed anywhere after the 3 required top-nodes" in {
      Room.isValidLocation(
        maybeDestId = Some(StorageNodeId(5)),
        pathTypes = Seq(
          StorageNodeId(1) -> RootType,
          StorageNodeId(2) -> OrganisationType,
          StorageNodeId(3) -> BuildingType,
          StorageNodeId(4) -> RoomType,
          StorageNodeId(5) -> StorageUnitType
        )
      ) mustBe true
    }

  }

  "A StorageUnit node" should {

    "not be allowed under a root node" in {
      StorageUnit.isValidLocation(
        maybeDestId = Some(StorageNodeId(1)),
        pathTypes = Seq(StorageNodeId(1) -> RootType)
      ) mustBe false
    }

    "not be allowed under a top-level organisation node" in {
      StorageUnit.isValidLocation(
        maybeDestId = Some(StorageNodeId(2)),
        pathTypes = Seq(
          StorageNodeId(1) -> RootType,
          StorageNodeId(2) -> OrganisationType
        )
      ) mustBe false
    }

    "be allowed under a top-level building node" in {
      StorageUnit.isValidLocation(
        maybeDestId = Some(StorageNodeId(3)),
        pathTypes = Seq(
          StorageNodeId(1) -> RootType,
          StorageNodeId(2) -> OrganisationType,
          StorageNodeId(3) -> BuildingType
        )
      ) mustBe true
    }

    "not be allowed to be placed when there's no destination Id" in {
      StorageUnit.isValidLocation(
        maybeDestId = None,
        pathTypes = Seq(
          StorageNodeId(1) -> RootType,
          StorageNodeId(2) -> OrganisationType,
          StorageNodeId(3) -> BuildingType
        )
      ) mustBe false
    }

    "be allowed anywhere after the 3 required top-nodes" in {
      StorageUnit.isValidLocation(
        maybeDestId = Some(StorageNodeId(5)),
        pathTypes = Seq(
          StorageNodeId(1) -> RootType,
          StorageNodeId(2) -> OrganisationType,
          StorageNodeId(3) -> BuildingType,
          StorageNodeId(4) -> RoomType,
          StorageNodeId(5) -> StorageUnitType
        )
      ) mustBe true
    }

  }

}
