package services.storage

import models.storage.{Interval, MovableObject}
import models.storage.Move.{MoveNodesCmd, MoveObjectsCmd}
import models.storage.event.EventType
import models.storage.event.EventTypeRegistry.TopLevelEvents.MoveObjectType
import models.storage.event.move.{MoveNode, MoveObject}
import models.storage.nodes._
import no.uio.musit.MusitResults.{MusitSuccess, MusitValidationError}
import no.uio.musit.models.ObjectTypes.CollectionObject
import no.uio.musit.models._
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import org.joda.time.DateTime
import utils.testhelpers.{BaseDummyData, EventGenerators, NodeGenerators}

class StorageNodeServiceSpec
    extends MusitSpecWithAppPerSuite
    with BaseDummyData
    with NodeGenerators
    with EventGenerators
    with MusitResultValues {

  val service = fromInstanceCache[StorageNodeService]

  private var insertedNodeIds: Map[StorageNodeDatabaseId, StorageNodeId] = Map.empty

  def saveRoot(r: RootNode, mid: MuseumId = defaultMuseumId) = {
    val ins = service.addRoot(mid, r).futureValue.successValue.value
    insertedNodeIds = insertedNodeIds ++ Map(ins.id.get -> ins.nodeId.get)
    ins
  }

  def saveOrg(o: Organisation, mid: MuseumId = defaultMuseumId) = {
    val ins = service.addOrganisation(mid, o).futureValue.successValue.value
    insertedNodeIds = insertedNodeIds ++ Map(ins.id.get -> ins.nodeId.get)
    ins
  }

  def saveBuilding(b: Building, mid: MuseumId = defaultMuseumId) = {
    val ins = service.addBuilding(mid, b).futureValue.successValue.value
    insertedNodeIds = insertedNodeIds ++ Map(ins.id.get -> ins.nodeId.get)
    ins
  }

  def saveRoom(r: Room, mid: MuseumId = defaultMuseumId) = {
    val ins = service.addRoom(mid, r).futureValue.successValue.value
    insertedNodeIds = insertedNodeIds ++ Map(ins.id.get -> ins.nodeId.get)
    ins
  }

  def saveUnit(u: StorageUnit, mid: MuseumId = defaultMuseumId) = {
    val ins = service.addStorageUnit(mid, u).futureValue.successValue.value
    insertedNodeIds = insertedNodeIds ++ Map(ins.id.get -> ins.nodeId.get)
    ins
  }

  def children(nid: StorageNodeDatabaseId, mid: MuseumId = defaultMuseumId) = {
    service.getChildren(mid, nid, 1, 10).futureValue.successValue.matches
  }

  // Initialize base data
  val baseIds      = bootstrapBaseStructure()
  val rootId       = baseIds.keys.head
  val rootUUID     = baseIds.values.head
  val orgId        = baseIds.keys.tail.head
  val orgUUID      = baseIds.values.tail.head
  val buildingId   = baseIds.keys.last
  val buildingUUID = baseIds.values.last

  insertedNodeIds = insertedNodeIds ++ baseIds

  "Using the StorageNodeService API" should {

    "successfully create a new room node with environment requirements" in {
      val room = createRoom(partOf = Some(buildingId))
      val ins  = saveRoom(room)

      ins.updatedBy.value mustBe defaultActorId
      ins.updatedDate.value.getYear mustBe DateTime.now.getYear
      ins.id must not be empty
      ins.environmentRequirement.value mustBe defaultEnvironmentRequirement
    }

    "successfully update a building with new environment requirements" in {
      val building = createBuilding(partOf = Some(orgId))

      val ins = saveBuilding(building)
      val someEnvReq = Some(
        initEnvironmentRequirement(
          hypoxic = Some(Interval[Double](44.4, Some(55)))
        )
      )
      val up  = ins.copy(environmentRequirement = someEnvReq)
      val res = service.updateBuilding(defaultMuseumId, ins.id.get, up).futureValue

      val updated = res.successValue.value
      updated.id mustBe ins.id
      updated.environmentRequirement mustBe someEnvReq
      updated.updatedBy.value mustBe defaultActorId
      updated.updatedDate.value.year().get() mustBe DateTime.now().year().get()
    }

    "successfully update a storage unit and fetch as StorageNode" in {
      val su  = createStorageUnit(partOf = Some(buildingId))
      val ins = saveUnit(su)

      val up  = ins.copy(name = "UggaBugga", areaTo = Some(4D))
      val res = service.updateStorageUnit(defaultMuseumId, ins.id.get, up).futureValue

      val updated = res.successValue.value
      updated.name mustBe "UggaBugga"
      updated.areaTo mustBe Some(4D)
      updated.updatedBy mustBe Some(defaultActorId)
      updated.updatedDate.value.year().get() mustBe DateTime.now().year().get()
    }

    "successfully mark a node as deleted" in {
      val su  = createStorageUnit(partOf = Some(buildingId))
      val ins = saveUnit(su)

      service.deleteNode(defaultMuseumId, ins.id.value).futureValue mustBe MusitSuccess(
        Some(1)
      )

      service
        .getNodeByDatabaseId(defaultMuseumId, ins.id.get)
        .futureValue
        .successValue mustBe empty
    }

    "not remove a node that has children" in {
      val su1  = createStorageUnit(partOf = Some(buildingId))
      val ins1 = saveUnit(su1)
      ins1.id must not be None

      val su2  = createStorageUnit(partOf = ins1.id)
      val ins2 = saveUnit(su2)
      ins2.id must not be None

      val res = service.deleteNode(defaultMuseumId, ins1.id.get).futureValue
      res.successValue.value mustBe -1
    }

    "not mark a node as deleted when wrong museumId is used" in {
      val su  = createStorageUnit(partOf = Some(buildingId))
      val ins = saveUnit(su)

      val wrongMid = MuseumId(4)

      service.deleteNode(wrongMid, ins.id.value).futureValue

      val res = service
        .getNodeById(defaultMuseumId, ins.nodeId.value)
        .futureValue
        .successValue
        .value
      res.id mustBe ins.id
      res.updatedBy mustBe Some(defaultActorId)
    }

    "not update a storage unit when using the wrong museumId" in {
      val wrongMid = MuseumId(4)
      val su       = createStorageUnit(partOf = Some(buildingId))
      val ins      = saveUnit(su)
      val upd      = ins.copy(name = "UggaBugga", areaTo = Some(4.0))

      service
        .updateStorageUnit(wrongMid, ins.id.value, upd)
        .futureValue
        .successValue mustBe None
    }

    "not update a building or environment requirements when using wrong museumID" in {
      val wrongMid = MuseumId(4)
      val b        = createBuilding(partOf = Some(orgId))
      val ins      = saveBuilding(b)
      val someEnvReq = Some(
        initEnvironmentRequirement(
          hypoxic = Some(Interval[Double](44.4, Some(55)))
        )
      )
      val upd = ins.copy(
        environmentRequirement = someEnvReq,
        address = Some("BortIStaurOgVeggAddress")
      )

      service
        .updateBuilding(wrongMid, ins.id.value, upd)
        .futureValue
        .successValue mustBe None

      val res = service.getBuildingById(defaultMuseumId, ins.id.get).futureValue
      res.successValue.value.address mustBe b.address
      res.successValue.value.updatedDate mustBe b.updatedDate
      res.successValue.value.updatedBy mustBe b.updatedBy
      res.successValue.value.environmentRequirement mustBe b.environmentRequirement
    }

    "not update a room when using wrong museumId" in {
      val wrongMid = MuseumId(4)
      val room     = createRoom(partOf = Some(buildingId))
      val ins      = saveRoom(room)
      val secAss   = ins.securityAssessment.copy(waterDamage = Some(true))
      val upd      = ins.copy(securityAssessment = secAss)

      service
        .updateRoom(wrongMid, ins.id.value, upd)
        .futureValue
        .successValue mustBe None

      val res = service.getRoomById(defaultMuseumId, ins.id.get).futureValue
      res.successValue.value.securityAssessment mustBe room.securityAssessment
      res.successValue.value.updatedDate mustBe room.updatedDate
      res.successValue.value.updatedBy mustBe room.updatedBy
      res.successValue.value.environmentRequirement mustBe room.environmentRequirement
    }

    "find the relevant rooms when searching with a valid MuseumId" in {
      val searchRoom =
        service.searchByName(defaultMuseumId, "FooRoom", 1, 25).futureValue
      searchRoom.successValue.head.name mustBe "FooRoom"
      searchRoom.successValue.size mustBe 5
    }

    "not find any rooms when searching with the wrong MuseumId" in {
      service
        .searchByName(MuseumId(4), "FooRoom", 1, 25)
        .futureValue
        .successValue
        .size mustBe 0
    }

    "fail when searching for a room with no search criteria" in {
      service.searchByName(defaultMuseumId, "", 1, 25).futureValue.isSuccess mustBe false
    }

    "fail when searching for a room with less than 3 characters" in {
      service
        .searchByName(defaultMuseumId, "Fo", 1, 25)
        .futureValue
        .isSuccess mustBe false
    }

    "successfully move a node and all its children" in {
      // Prepare some nodes
      val b1 = saveBuilding(createBuilding(name = "Building1", partOf = Some(orgId)))
      val b2 = saveBuilding(createBuilding(name = "Building2", partOf = Some(orgId)))
      val u1 = saveUnit(createStorageUnit(name = "Unit1", partOf = b1.id))
      val u2 = saveUnit(createStorageUnit(name = "Unit2", partOf = u1.id))
      val u3 = saveUnit(createStorageUnit(name = "Unit3", partOf = u1.id))
      val u4 = saveUnit(createStorageUnit(name = "Unit4", partOf = u3.id))

      val directChildren = children(u1.id.get)
      val grandChildren = directChildren.flatMap { c =>
        children(c.id.get)
      }
      val mostChildren = directChildren ++ grandChildren

      val move = MoveNodesCmd(
        destination = b2.nodeId.value,
        items = Seq(u1.nodeId.value)
      )

      val event = MoveNode.fromCommand(defaultActorId, move)

      service
        .moveNodes(defaultMuseumId, b2.nodeId.value, event)
        .futureValue
        .isSuccess mustBe true

      mostChildren.map { c =>
        service.getNodeByDatabaseId(defaultMuseumId, c.id.value).futureValue.map { n =>
          n.value.path must not be None
          n.value.path.path must startWith(b2.path.path)
        }
      }
    }

    "successfully move an object with a previous location" in {
      val oid  = ObjectUUID.unsafeFromString("e2cdc938-70d0-44f8-89b5-ae9387e1cc61")
      val dest = insertedNodeIds(StorageNodeDatabaseId(20))

      val cmd    = MoveObjectsCmd(dest, Seq(MovableObject(oid, CollectionObject)))
      val events = MoveObject.fromCommand(defaultActorId, cmd)

      val res =
        service.moveObjects(defaultMuseumId, dest, events).futureValue.successValue

      val loc2 =
        service.currentObjectLocation(defaultMuseumId, oid, CollectionObject).futureValue
      loc2.successValue.value.id mustBe Some(StorageNodeDatabaseId(20))
      loc2.successValue.value.pathNames must not be empty
    }

    "not register a move when current location and destination are the same" in {
      val oid  = ObjectUUID.unsafeFromString("e2cdc938-70d0-44f8-89b5-ae9387e1cc61")
      val dest = insertedNodeIds(StorageNodeDatabaseId(20))

      val cmd    = MoveObjectsCmd(dest, Seq(MovableObject(oid, CollectionObject)))
      val events = MoveObject.fromCommand(defaultActorId, cmd)

      service
        .moveObjects(defaultMuseumId, dest, events)
        .futureValue
        .isFailure mustBe true

      service
        .currentObjectLocation(defaultMuseumId, oid, CollectionObject)
        .futureValue
        .successValue
        .value
        .nodeId mustBe Some(dest)
    }

    "successfully move an object with no previous location" in {
      val oid  = ObjectUUID.generate()
      val dest = buildingUUID

      val cmd    = MoveObjectsCmd(dest, Seq(MovableObject(oid, CollectionObject)))
      val events = MoveObject.fromCommand(defaultActorId, cmd)

      service
        .moveObjects(defaultMuseumId, dest, events)
        .futureValue
        .isSuccess mustBe true

      service
        .currentObjectLocation(defaultMuseumId, oid, CollectionObject)
        .futureValue
        .successValue
        .value
        .nodeId mustBe Some(dest)
    }

    "get current location for an object" in {
      val oid = ObjectUUID.unsafeFromString("e2cdc938-70d0-44f8-89b5-ae9387e1cc61")
      val loc = insertedNodeIds(StorageNodeDatabaseId(20))

      service
        .currentObjectLocation(defaultMuseumId, oid, CollectionObject)
        .futureValue
        .successValue
        .value
        .nodeId mustBe Some(loc)
    }

  }

  "Validating a storage node destination" should {
    // Bootstrap some test strucutures
    // scalastyle:off
    // format: off
    val room1 = service.addRoom(defaultMuseumId, createRoom(partOf = Some(buildingId))).futureValue.successValue.value
    val room2 = service.addRoom(defaultMuseumId, createRoom(partOf = Some(buildingId))).futureValue.successValue.value
    val room3 = service.addRoom(defaultMuseumId, createRoom(partOf = Some(buildingId))).futureValue.successValue.value
    val unit1 = service.addStorageUnit(defaultMuseumId, createStorageUnit(partOf = room1.id)).futureValue.successValue.value
    val unit2 = service.addStorageUnit(defaultMuseumId, createStorageUnit(partOf = room1.id)).futureValue.successValue.value
    // scalastyle:on
    // format: on

    "not be valid when the destination is a child of the current node" in {
      val r = service.validatePosition(defaultMuseumId, room1, unit2.path).futureValue
      r mustBe MusitValidationError("Illegal destination")
    }

    "not be valid when the destination is an empty node" in {
      // format: off
      val r = service.validatePosition(defaultMuseumId, room1, NodePath.empty).futureValue
      r mustBe MusitValidationError("Illegal move")
      // format: on
    }

    "be valid when the destination is not a child of the current node" in {
      val r = service.validatePosition(defaultMuseumId, unit1, room3.path).futureValue
      r mustBe MusitSuccess(())
    }

  }
}
