package repositories.storage.dao

import models.storage.MovableObject_Old
import no.uio.musit.models.ObjectTypes.{CollectionObjectType, Node, SampleObjectType}
import no.uio.musit.models.{ObjectId, StorageNodeDatabaseId}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import repositories.storage.old_dao.{LocalObjectDao => LocalObjectDaoOld}

class LocalObjectDaoSpec extends MusitSpecWithAppPerSuite with MusitResultValues {

  val dao = fromInstanceCache[LocalObjectDaoOld]

  "LocalObjectDao" should {
    "not return location for the movable objects when ObjectType doesn't match" in {
      val obj = MovableObject_Old(ObjectId(20), SampleObjectType)
      val res = dao.currentLocationsForMovableObjects(Seq(obj)).futureValue

      res.successValue mustBe Map(obj -> None)
    }

    "find location for the movable object when the object has a matching ObjectType" in {
      val obj = MovableObject_Old(ObjectId(20), CollectionObjectType)
      val res = dao.currentLocationsForMovableObjects(Seq(obj)).futureValue

      res.successValue mustBe Map(obj -> Some(StorageNodeDatabaseId(8)))
    }

    "find location for the movable object with multiple matching ObjectTypes" in {
      val objOne = MovableObject_Old(ObjectId(20), CollectionObjectType)
      val objTwo = MovableObject_Old(ObjectId(21), SampleObjectType)

      val res = dao.currentLocationsForMovableObjects(Seq(objOne, objTwo)).futureValue

      res.successValue mustBe Map(
        objOne -> Some(StorageNodeDatabaseId(8)),
        objTwo -> Some(StorageNodeDatabaseId(9))
      )
    }

    "not find location for movable object when ObjectType isn't provided" in {
      val obj = MovableObject_Old(ObjectId(13), Node)
      val res = dao.currentLocationsForMovableObjects(Seq(obj)).futureValue

      res.successValue mustBe Map(obj -> None)
    }
  }

}
