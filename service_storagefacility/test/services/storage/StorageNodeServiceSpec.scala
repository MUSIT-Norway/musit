package services.storage

import models.storage.Interval
import models.storage.Move.MoveNodesCmd
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

      val loc1 =
        service.currentObjectLocation(defaultMuseumId, oid, CollectionObject).futureValue
      loc1.successValue.value.id mustBe Some(StorageNodeDatabaseId(5))

      val event = MoveObject(
        id = None,
        doneBy = Some(defaultActorId),
        doneDate = DateTime.now,
        affectedThing = Some(oid),
        registeredBy = Some(defaultActorId),
        registeredDate = Some(DateTime.now),
        eventType = EventType.fromEventTypeId(MoveObjectType.id),
        objectType = CollectionObject,
        from = StorageNodeId.fromString("01134afe-b262-434b-a71f-8f697bc75e56"),
        to = dest
      )

      val res =
        service.moveObjects(defaultMuseumId, dest, Seq(event)).futureValue.successValue

      val loc2 =
        service.currentObjectLocation(defaultMuseumId, oid, CollectionObject).futureValue
      loc2.successValue.value.id mustBe Some(StorageNodeDatabaseId(20))
      loc2.successValue.value.pathNames must not be empty
    }

    "not register a move when current location and destination are the same" in {
      pending
    }

    "successfully move an object with no previous location" in {
      pending
    }

    "not mark a node as deleted when wrong museumId is used" in {
      pending
    }

    "not update a storage unit when using the wrong museumId" in {
      pending
    }

    "not update a building or environment requirements when using wrong museumID" in {
      pending
    }

    "not update a room when using wrong museumId" in {
      pending
    }

    "get current location for an object" in {
      pending
    }

    "find the relevant rooms when searching with a valid MuseumId" in {
      pending
    }

    "not find any rooms when searching with the wrong MuseumId" in {
      pending
    }

    "fail when searching for a room with no search criteria" in {
      pending
    }

    "fail when searching for a room with less than 3 characters" in {
      pending
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
