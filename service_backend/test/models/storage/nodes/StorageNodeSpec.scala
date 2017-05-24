package models.storage.nodes

import models.storage.nodes.StorageType._
import no.uio.musit.models.{NodePath, StorageNodeDatabaseId}
import org.scalatest.{MustMatchers, WordSpec}

class StorageNodeSpec extends WordSpec with MustMatchers {

  "A Root node" should {

    "be allowed at the top location in a node hierarchy" in {
      RootNode.isValidLocation(NodePath.empty) mustBe true
    }

    "not be valid other than top location in a node hierarchy" in {
      RootNode.isValidLocation(NodePath(",1,2,3,")) mustBe false
    }

  }

  "An Organisation node" should {

    "not be allowed to be placed when there's no destination Id" in {
      Organisation.isValidLocation(
        maybeDestId = None,
        pathTypes = Seq(StorageNodeDatabaseId(1) -> RootType)
      ) mustBe false
    }

    "be allowed directly under a root node" in {
      Organisation.isValidLocation(
        maybeDestId = Some(StorageNodeDatabaseId(1)),
        pathTypes = Seq(StorageNodeDatabaseId(1) -> RootType)
      ) mustBe true
    }

    "not be allowed under a top-level organisation node" in {
      Organisation.isValidLocation(
        maybeDestId = Some(StorageNodeDatabaseId(2)),
        pathTypes = Seq(
          StorageNodeDatabaseId(1) -> RootType,
          StorageNodeDatabaseId(2) -> OrganisationType
        )
      ) mustBe false
    }

    "be allowed under a top-level building node" in {
      Organisation.isValidLocation(
        maybeDestId = Some(StorageNodeDatabaseId(3)),
        pathTypes = Seq(
          StorageNodeDatabaseId(1) -> RootType,
          StorageNodeDatabaseId(2) -> OrganisationType,
          StorageNodeDatabaseId(3) -> BuildingType
        )
      ) mustBe true
    }

    "be allowed anywhere after the 3 required top-nodes" in {
      Organisation.isValidLocation(
        maybeDestId = Some(StorageNodeDatabaseId(5)),
        pathTypes = Seq(
          StorageNodeDatabaseId(1) -> RootType,
          StorageNodeDatabaseId(2) -> OrganisationType,
          StorageNodeDatabaseId(3) -> BuildingType,
          StorageNodeDatabaseId(4) -> RoomType,
          StorageNodeDatabaseId(5) -> StorageUnitType
        )
      ) mustBe true
    }

  }

  "A Building node" should {

    "not be allowed directly under a root node" in {
      Building.isValidLocation(
        maybeDestId = Some(StorageNodeDatabaseId(1)),
        pathTypes = Seq(StorageNodeDatabaseId(1) -> RootType)
      ) mustBe false
    }

    "be allowed under a top-level organisation node" in {
      Building.isValidLocation(
        maybeDestId = Some(StorageNodeDatabaseId(2)),
        pathTypes = Seq(
          StorageNodeDatabaseId(1) -> RootType,
          StorageNodeDatabaseId(2) -> OrganisationType
        )
      ) mustBe true
    }

    "not be allowed to be placed when there's no destination Id" in {
      Building.isValidLocation(
        maybeDestId = None,
        pathTypes = Seq(
          StorageNodeDatabaseId(1) -> RootType,
          StorageNodeDatabaseId(2) -> OrganisationType
        )
      ) mustBe false
    }

    "be allowed under a top-level building node" in {
      Building.isValidLocation(
        maybeDestId = Some(StorageNodeDatabaseId(3)),
        pathTypes = Seq(
          StorageNodeDatabaseId(1) -> RootType,
          StorageNodeDatabaseId(2) -> OrganisationType,
          StorageNodeDatabaseId(3) -> BuildingType
        )
      ) mustBe true
    }

    "be allowed anywhere after the 3 required top-nodes" in {
      Building.isValidLocation(
        maybeDestId = Some(StorageNodeDatabaseId(5)),
        pathTypes = Seq(
          StorageNodeDatabaseId(1) -> RootType,
          StorageNodeDatabaseId(2) -> OrganisationType,
          StorageNodeDatabaseId(3) -> BuildingType,
          StorageNodeDatabaseId(4) -> RoomType,
          StorageNodeDatabaseId(5) -> StorageUnitType
        )
      ) mustBe true
    }
  }

  "A Room node" should {

    "not be allowed under a root node" in {
      Room.isValidLocation(
        maybeDestId = Some(StorageNodeDatabaseId(1)),
        pathTypes = Seq(StorageNodeDatabaseId(1) -> RootType)
      ) mustBe false
    }

    "not be allowed under a top-level organisation node" in {
      Room.isValidLocation(
        maybeDestId = Some(StorageNodeDatabaseId(2)),
        pathTypes = Seq(
          StorageNodeDatabaseId(1) -> RootType,
          StorageNodeDatabaseId(2) -> OrganisationType
        )
      ) mustBe false
    }

    "be allowed under a top-level building node" in {
      Room.isValidLocation(
        maybeDestId = Some(StorageNodeDatabaseId(3)),
        pathTypes = Seq(
          StorageNodeDatabaseId(1) -> RootType,
          StorageNodeDatabaseId(2) -> OrganisationType,
          StorageNodeDatabaseId(3) -> BuildingType
        )
      ) mustBe true
    }

    "not be allowed to be placed when there's no destination Id" in {
      Room.isValidLocation(
        maybeDestId = None,
        pathTypes = Seq(
          StorageNodeDatabaseId(1) -> RootType,
          StorageNodeDatabaseId(2) -> OrganisationType,
          StorageNodeDatabaseId(3) -> BuildingType
        )
      ) mustBe false
    }

    "be allowed anywhere after the 3 required top-nodes" in {
      Room.isValidLocation(
        maybeDestId = Some(StorageNodeDatabaseId(5)),
        pathTypes = Seq(
          StorageNodeDatabaseId(1) -> RootType,
          StorageNodeDatabaseId(2) -> OrganisationType,
          StorageNodeDatabaseId(3) -> BuildingType,
          StorageNodeDatabaseId(4) -> RoomType,
          StorageNodeDatabaseId(5) -> StorageUnitType
        )
      ) mustBe true
    }

  }

  "A StorageUnit node" should {

    "not be allowed under a root node" in {
      StorageUnit.isValidLocation(
        maybeDestId = Some(StorageNodeDatabaseId(1)),
        pathTypes = Seq(StorageNodeDatabaseId(1) -> RootType)
      ) mustBe false
    }

    "not be allowed under a top-level organisation node" in {
      StorageUnit.isValidLocation(
        maybeDestId = Some(StorageNodeDatabaseId(2)),
        pathTypes = Seq(
          StorageNodeDatabaseId(1) -> RootType,
          StorageNodeDatabaseId(2) -> OrganisationType
        )
      ) mustBe false
    }

    "be allowed under a top-level building node" in {
      StorageUnit.isValidLocation(
        maybeDestId = Some(StorageNodeDatabaseId(3)),
        pathTypes = Seq(
          StorageNodeDatabaseId(1) -> RootType,
          StorageNodeDatabaseId(2) -> OrganisationType,
          StorageNodeDatabaseId(3) -> BuildingType
        )
      ) mustBe true
    }

    "not be allowed to be placed when there's no destination Id" in {
      StorageUnit.isValidLocation(
        maybeDestId = None,
        pathTypes = Seq(
          StorageNodeDatabaseId(1) -> RootType,
          StorageNodeDatabaseId(2) -> OrganisationType,
          StorageNodeDatabaseId(3) -> BuildingType
        )
      ) mustBe false
    }

    "be allowed anywhere after the 3 required top-nodes" in {
      StorageUnit.isValidLocation(
        maybeDestId = Some(StorageNodeDatabaseId(5)),
        pathTypes = Seq(
          StorageNodeDatabaseId(1) -> RootType,
          StorageNodeDatabaseId(2) -> OrganisationType,
          StorageNodeDatabaseId(3) -> BuildingType,
          StorageNodeDatabaseId(4) -> RoomType,
          StorageNodeDatabaseId(5) -> StorageUnitType
        )
      ) mustBe true
    }

  }

}
