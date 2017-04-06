package repositories.dao.caching

import models.MovableObject
import no.uio.musit.models.ObjectTypes.{CollectionObject, Node, SampleObject}
import no.uio.musit.models.{ObjectId, StorageNodeDatabaseId}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues

class LocalObjectDaoSpec extends MusitSpecWithAppPerSuite with MusitResultValues {

  val dao = fromInstanceCache[LocalObjectDao]

  "LocalObjectDao" should {
    "not return location for the movable objects when ObjectType doesn't match" in {
      val obj = MovableObject(ObjectId(20), SampleObject)
      val res = dao.currentLocationsForMovableObjects(Seq(obj)).futureValue

      res.successValue mustBe Map(obj -> None)
    }

    "find location for the movable object when the object has a matching ObjectType" in {
      val obj = MovableObject(ObjectId(20), CollectionObject)
      val res = dao.currentLocationsForMovableObjects(Seq(obj)).futureValue

      res.successValue mustBe Map(obj -> Some(StorageNodeDatabaseId(8)))
    }

    "find location for the movable object when the object has a matching multiple ObjectTypes" in {
      val objOne = MovableObject(ObjectId(20), CollectionObject)
      val objTwo = MovableObject(ObjectId(21), SampleObject)
      val res    = dao.currentLocationsForMovableObjects(Seq(objOne, objTwo)).futureValue

      res.successValue mustBe Map(
        objOne -> Some(StorageNodeDatabaseId(8)),
        objTwo -> Some(StorageNodeDatabaseId(9))
      )
    }

    "not find location for movable object when ObjectType isn't provided for the object" in {
      val obj = MovableObject(ObjectId(13), Node)
      val res = dao.currentLocationsForMovableObjects(Seq(obj)).futureValue

      res.successValue mustBe Map(obj -> None)
    }
  }

}
