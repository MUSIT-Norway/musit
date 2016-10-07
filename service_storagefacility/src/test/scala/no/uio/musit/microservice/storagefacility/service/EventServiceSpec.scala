package no.uio.musit.microservice.storagefacility.service

  import no.uio.musit.microservice.storagefacility.domain.event.control.Control
  import no.uio.musit.microservice.storagefacility.domain.event.dto.DtoConverters
  import no.uio.musit.microservice.storagefacility.domain.event.move.MoveNode
  import no.uio.musit.microservice.storagefacility.domain.storage.{EnvironmentRequirement, Root, StorageNodeId, StorageUnit}
  import no.uio.musit.microservice.storagefacility.domain.{Interval, Move, MuseumId}
  import no.uio.musit.microservice.storagefacility.testhelpers.{EventGenerators, NodeGenerators}
  import no.uio.musit.test.MusitSpecWithAppPerSuite
  import org.scalatest.time.{Millis, Seconds, Span}

  class EventServiceSpec  extends MusitSpecWithAppPerSuite with NodeGenerators
  with EventGenerators {

    implicit override val patienceConfig: PatienceConfig = PatienceConfig(
      timeout = Span(15, Seconds),
      interval = Span(50, Millis)
    )

    implicit val DummyUser = "Bevel Lemelisk"

    val controlService: ControlService = fromInstanceCache[ControlService]
    val obsService: ObservationService = fromInstanceCache[ObservationService]
    val storageNodeService: StorageNodeService = fromInstanceCache[StorageNodeService]

    // This is mutable to allow keeping track of the last inserted eventId.
    private var latestEventId: Long = _


    "With MuseumId, processing events" should {
      "Unsuccessfully when inserting a Control with wrong museumId" in {
        val mid = MuseumId(2)
        val ctrl = createControl(defaultBuilding.id)
        val controlEvent = controlService.add(mid, defaultBuilding.id.get, ctrl).futureValue
        controlEvent.isSuccess mustBe true
        controlEvent.get.baseEvent.id.get mustBe 1
        latestEventId = controlEvent.get.baseEvent.id.get
        latestEventId mustBe 1L

        val anotherMid = MuseumId(4)
        val currentControlEvent = controlService.add(anotherMid, defaultBuilding.id.get, ctrl).futureValue
        currentControlEvent.isSuccess mustBe false
        currentControlEvent.isFailure mustBe true
      }

      "Unsuccessfully when inserting a Observation with wrong museumId" in {
        val mid = MuseumId(2)
        val ctrl = createObservation(defaultBuilding.id)
        val obsEvent = obsService.add(mid, defaultBuilding.id.get, ctrl).futureValue
        obsEvent.isSuccess mustBe true
        obsEvent.get.baseEvent.id.get mustBe 9

        latestEventId = obsEvent.get.baseEvent.id.get
        latestEventId mustBe 9L

        val anotherMid = MuseumId(4)
        val currentObsEvent = obsService.add(anotherMid, defaultBuilding.id.get, ctrl).futureValue
        currentObsEvent.isSuccess mustBe false
        currentObsEvent.isFailure mustBe true
      }
      "Unsuccessfully when inserting a EnvReq with wrong museumId" in {
        val mid = MuseumId(2)
        val envReq = createEnvRequirement(defaultBuilding.id)
        //val envReqEvent = storageNodeService.saveEnvReq(mid,defaultBuilding.id.get,envReq.asInstanceOf[EnvironmentRequirement]).futureValue
        // TODO: problem solving this test on envReqEvent against museumID
       /* envReqEvent.get mustBe true
        envReqEvent.get. mustBe 9
        println("inni envReq")
        latestEventId = obsEvent.get.baseEvent.id.get
        latestEventId mustBe 9L

        val anotherMid = MuseumId(4)
        val currentObsEvent = obsService.add(anotherMid, defaultBuilding.id.get, ctrl).futureValue
        currentObsEvent.isSuccess mustBe false
        currentObsEvent.isFailure mustBe true*/
      }

    }
  }

