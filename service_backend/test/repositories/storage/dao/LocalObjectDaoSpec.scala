package repositories.storage.dao

import no.uio.musit.models.ObjectTypes.{CollectionObjectType, Node, SampleObjectType}
import no.uio.musit.models.{ObjectUUID, StorageNodeId}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues

class LocalObjectDaoSpec extends MusitSpecWithAppPerSuite with MusitResultValues {

  val dao = fromInstanceCache[LocalObjectDao]

  val objectId1 = ObjectUUID.unsafeFromString("3a71f423-52b2-4437-a62b-6e37ad406bcd")
  val objectId2 = ObjectUUID.unsafeFromString("baab2f60-4f49-40fe-99c8-174b13b12d46")
  val sampleObjectId1 =
    ObjectUUID.unsafeFromString("28cf7c75-66b2-4991-b871-d92baeec0049")

  val location1 = StorageNodeId.unsafeFromString("244f09a3-eb1a-49e7-80ee-7a07baa016dd")
  val location2 = StorageNodeId.unsafeFromString("01134afe-b262-434b-a71f-8f697bc75e56")

  "LocalObjectDao" should {
    "find location for a list of ObjectUUIDs of different object types" in {
      val res =
        dao.currentLocations(Seq(objectId1, sampleObjectId1)).futureValue.successValue

      res.size mustBe 2
      res mustBe Map(
        objectId1       -> Some(location1),
        sampleObjectId1 -> Some(location2)
      )
    }

    "find location for a single collection object" in {
      val res =
        dao.currentLocation(objectId1, CollectionObjectType).futureValue.successValue

      res mustBe Some(location1)
    }

    "find a location for a single sample object" in {
      val res =
        dao.currentLocation(objectId2, SampleObjectType).futureValue.successValue

      res mustBe Some(location1)
    }

    "return an empty result when object has no location" in {
      val noSuchObject = ObjectUUID.generate()
      val res          = dao.currentLocations(Seq(noSuchObject)).futureValue

      res.successValue mustBe Map(noSuchObject -> None)
    }

    "return an empty result when object type is wrong" in {
      val res =
        dao.currentLocation(objectId1, Node).futureValue.successValue

      res mustBe None
    }
  }

}
