package repositories.storage.dao.nodes

import models.storage.nodes.StorageType._
import models.storage.nodes.{Root, RootLoan, StorageType}
import no.uio.musit.MusitResults.MusitSuccess
import no.uio.musit.models._
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import org.joda.time.DateTime
import utils.testdata.NodeGenerators

class StorageUnitDaoSpec
    extends MusitSpecWithAppPerSuite
    with NodeGenerators
    with MusitResultValues {

  val insertedNodeIds = Map.newBuilder[Long, StorageNodeId]

  "StorageUnitDao" should {

    "succeed when inserting several root nodes" in {

      def createRoot(name: String): Root = Root(
        nodeId = StorageNodeId.generateAsOpt(),
        name = name,
        updatedBy = Some(defaultActorId),
        updatedDate = Some(DateTime.now())
      )

      def createRootLoan(name: String): RootLoan = RootLoan(
        nodeId = StorageNodeId.generateAsOpt(),
        name = name,
        updatedBy = Some(defaultActorId),
        updatedDate = Some(DateTime.now())
      )

      for (i <- 23 to 25) {
        val r     = createRoot(s"root$i")
        val insId = storageUnitDao.insertRoot(defaultMuseumId, r).futureValue

        insId mustBe MusitSuccess(StorageNodeDatabaseId(i.toLong))

        insertedNodeIds += insId.successValue.underlying -> r.nodeId.value
      }
      val anotherMid = MuseumId(4)
      for (i <- 26 to 27) {
        val r     = createRootLoan(s"rootLoan$i")
        val insId = storageUnitDao.insertRoot(anotherMid, r).futureValue

        insId.successValue mustBe StorageNodeDatabaseId(i.toLong)
        insertedNodeIds += insId.successValue.underlying -> r.nodeId.value
      }
    }

    "succeed when inserting a new storage unit" in {
      val path  = NodePath(",1,2,3,4,")
      val su    = createStorageUnit(path = path)
      val insId = storageUnitDao.insert(defaultMuseumId, su).futureValue
      insId.successValue mustBe a[StorageNodeDatabaseId]

      insertedNodeIds += insId.successValue.underlying -> su.nodeId.value
    }

    "successfully fetch a storage unit" in {
      val mid   = MuseumId(5)
      val su    = createStorageUnit()
      val insId = storageUnitDao.insert(mid, su).futureValue
      insId.successValue mustBe a[StorageNodeDatabaseId]

      insertedNodeIds += insId.successValue.underlying -> su.nodeId.value

      val res = storageUnitDao.getByDatabaseId(mid, insId.get).futureValue

      res.successValue.value.storageType mustBe su.storageType
      res.successValue.value.name mustBe su.name
    }

    "successfully update a storage unit and fetch as StorageNode" in {
      val mid   = MuseumId(5)
      val su    = createStorageUnit()
      val insId = storageUnitDao.insert(mid, su).futureValue
      insId.successValue mustBe a[StorageNodeDatabaseId]

      insertedNodeIds += insId.successValue.underlying -> su.nodeId.value

      val res = storageUnitDao.getByDatabaseId(mid, insId.get).futureValue

      res.successValue.value.storageType mustBe su.storageType
      res.successValue.value.name mustBe su.name

      val upd = res.successValue.value.copy(name = "UggaBugga", areaTo = Some(4.0))

      val updRes =
        storageUnitDao.update(mid, su.nodeId.get, upd).futureValue
      updRes.successValue.value mustBe 1

      val again = storageUnitDao.getByDatabaseId(mid, insId.get).futureValue
      again.successValue.value.name mustBe "UggaBugga"
      again.successValue.value.areaTo mustBe Some(4.0)
    }

    "successfully list root nodes" in {
      val nodes = storageUnitDao.findRootNodes(defaultMuseumId).futureValue

      nodes.successValue.foreach { n =>
        n.storageType.entryName must startWith("Root")
      }
    }

    "fail to list root nodes when museumId is wrong" in {
      val mid   = MuseumId(5)
      val nodes = storageUnitDao.findRootNodes(mid).futureValue

      nodes.successValue.size mustBe 0
      nodes.successValue.foreach(_.storageType mustBe StorageType.RootType)
    }

    "fail to list root nodes with museumId that does not exists" in {
      val mid   = MuseumId(55)
      val nodes = storageUnitDao.findRootNodes(mid).futureValue

      nodes.successValue.size mustBe 0
      nodes.successValue.foreach(_.storageType mustBe StorageType.RootType)
    }

    "successfully mark a node as deleted" in {
      val su    = createStorageUnit()
      val insId = storageUnitDao.insert(defaultMuseumId, su).futureValue
      insId.successValue mustBe a[StorageNodeDatabaseId]

      insertedNodeIds += insId.successValue.underlying -> su.nodeId.value

      val deleted = storageUnitDao
        .markAsDeleted(defaultActorId, defaultMuseumId, su.nodeId.get)
        .futureValue

      deleted.successValue mustBe 1

      val res = storageUnitDao.getByDatabaseId(defaultMuseumId, insId.get).futureValue
      res.successValue mustBe None
    }

    "successfully fetch the named path elements for a storage node" in {
      val path1 = NodePath(",23,32,")
      val su1 = createStorageUnit(
        partOf = Some(StorageNodeDatabaseId(23)),
        path = path1
      ).copy(name = "node1")
      val insId1 = storageUnitDao.insert(defaultMuseumId, su1).futureValue
      insId1.successValue mustBe StorageNodeDatabaseId(32)

      insertedNodeIds += insId1.successValue.underlying -> su1.nodeId.value

      val path2 = path1.appendChild(StorageNodeDatabaseId(33))
      val su2 = createStorageUnit(
        partOf = Some(insId1.get),
        path = path2
      ).copy(name = "node2")
      val insId2 = storageUnitDao.insert(defaultMuseumId, su2).futureValue
      insId2.successValue mustBe StorageNodeDatabaseId(33)

      insertedNodeIds += insId2.successValue.underlying -> su2.nodeId.value

      val res = storageUnitDao.namesForPath(path2).futureValue
      res.successValue.size mustBe 3
      res.successValue.head.nodeId mustBe StorageNodeDatabaseId(23)
      res.successValue.head.name mustBe "root23"
      res.successValue.tail.head.nodeId mustBe StorageNodeDatabaseId(32)
      res.successValue.tail.head.name mustBe "node1"
      res.successValue.last.nodeId mustBe StorageNodeDatabaseId(33)
      res.successValue.last.name mustBe "node2"
    }

    "fail to fetch a storage unit with wrong museumId" in {
      val mid   = MuseumId(5)
      val su    = createStorageUnit()
      val insId = storageUnitDao.insert(mid, su).futureValue
      insId.successValue mustBe a[StorageNodeDatabaseId]
      insertedNodeIds += insId.successValue.underlying -> su.nodeId.value

      val wrongMid = MuseumId(4)
      val res      = storageUnitDao.getByDatabaseId(mid, insId.get).futureValue

      res.successValue.value.storageType mustBe su.storageType
      res.successValue.value.name mustBe su.name
    }

    "fail to update a storage unit when museumId is wrong" in {
      val mid   = MuseumId(5)
      val su    = createStorageUnit()
      val insId = storageUnitDao.insert(mid, su).futureValue
      insId.successValue mustBe a[StorageNodeDatabaseId]
      insertedNodeIds += insId.successValue.underlying -> su.nodeId.value

      val res = storageUnitDao.getByDatabaseId(mid, insId.get).futureValue

      res.successValue.value.storageType mustBe su.storageType
      res.successValue.value.name must include("FooUnit")
      res.successValue.value.name mustBe su.name
      res.successValue.value.areaTo mustBe Some(2.0)

      val upd = res.successValue.value.copy(name = "UggaBugga", areaTo = Some(4.0))

      val anotherMid = MuseumId(4)
      val updRes =
        storageUnitDao.update(anotherMid, su.nodeId.get, upd).futureValue
      updRes.successValue mustBe None

      val again = storageUnitDao.getByDatabaseId(mid, insId.get).futureValue

      again.successValue.value.name mustBe "FooUnit"
      again.successValue.value.areaTo mustBe Some(2.0)
    }

    "fail to mark a node as deleted when museumId is wrong" in {
      val su    = createStorageUnit()
      val insId = storageUnitDao.insert(defaultMuseumId, su).futureValue
      insId.successValue mustBe a[StorageNodeDatabaseId]
      insertedNodeIds += insId.successValue.underlying -> su.nodeId.value

      val anotherMid = MuseumId(4)
      val deleted = storageUnitDao
        .markAsDeleted(defaultActorId, anotherMid, su.nodeId.get)
        .futureValue
      deleted.isFailure mustBe true

      val res =
        storageUnitDao.getByDatabaseId(defaultMuseumId, insId.successValue).futureValue
      res.successValue.value.id mustBe Some(insId.get)
    }

    "fetch tuples of StorageNodeId and StorageType for a NodePath" in {
      val orgPath = NodePath(",1,37,")
      val org = createOrganisation(
        partOf = Some(StorageNodeDatabaseId(1)),
        path = orgPath
      ).copy(name = "node-x")
      val organisationId = organisationDao.insert(defaultMuseumId, org).futureValue
      insertedNodeIds += organisationId.successValue.underlying -> org.nodeId.value

      val buildingPath = NodePath(",1,37,38,")
      val building = createBuilding(
        partOf = Some(organisationId.get),
        path = buildingPath
      )
      val buildingId = buildingDao.insert(defaultMuseumId, building).futureValue
      insertedNodeIds += buildingId.successValue.underlying -> building.nodeId.value

      val roomPath = NodePath(",1,37,38,39,")
      val room = createRoom(
        partOf = Some(buildingId.get),
        path = roomPath
      )
      val roomId = roomDao.insert(defaultMuseumId, room).futureValue
      insertedNodeIds += roomId.successValue.underlying -> room.nodeId.value

      val su1Path = NodePath(",1,37,38,39,40,")
      val su1 = createStorageUnit(
        partOf = Some(roomId.get),
        path = su1Path
      )
      val suId = storageUnitDao.insert(defaultMuseumId, su1).futureValue
      insertedNodeIds += suId.successValue.underlying -> su1.nodeId.value

      val expected = Seq(
        StorageNodeDatabaseId(1)    -> RootType,
        organisationId.successValue -> OrganisationType,
        buildingId.successValue     -> BuildingType,
        roomId.successValue         -> RoomType,
        suId.successValue           -> StorageUnitType
      )

      val tuples =
        storageUnitDao.getStorageTypesInPath(defaultMuseumId, su1Path).futureValue

      tuples.successValue must contain theSameElementsInOrderAs expected
    }

    "find all children for a given node" in {
      val nodeId = insertedNodeIds.result()(37)
      val res    = storageUnitDao.getChildren(defaultMuseumId, nodeId, 1, 10).futureValue
      val nids   = res.successValue.matches.flatMap(_.nodeId)
      nids must contain(insertedNodeIds.result()(38))
    }

    "successfully get a node when searching for name and not if it's wrong museumId" in {
      val mid = MuseumId(5)
      val getNodeName =
        storageUnitDao.getStorageNodeByName(mid, "Foo", 1, 25).futureValue

      getNodeName.successValue.size mustBe 3
      getNodeName.successValue.head.name must include("Foo")
      getNodeName.successValue.lift(2).value.name must include("Foo")

      val anotherMid = MuseumId(4)
      val notGetNodeName =
        storageUnitDao.getStorageNodeByName(anotherMid, "Foo", 1, 25).futureValue

      notGetNodeName mustBe a[MusitSuccess[_]]
      notGetNodeName.successValue.size mustBe 0
    }

    "fail when searching for name without invalid criteria" in {
      val mid         = MuseumId(5)
      val getNodeName = storageUnitDao.getStorageNodeByName(mid, "", 1, 25).futureValue

      getNodeName.successValue.size mustBe 0

      val tooFewLettersInSearchStr =
        storageUnitDao.getStorageNodeByName(mid, "", 1, 25).futureValue
      tooFewLettersInSearchStr.successValue.size mustBe 0

      val anotherMid = MuseumId(4)
      val noNodeName =
        storageUnitDao.getStorageNodeByName(anotherMid, "Foo", 1, 25).futureValue
      noNodeName.successValue.size mustBe 0
    }
  }

}
