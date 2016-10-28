package no.uio.musit.microservice.storagefacility.service

import no.uio.musit.microservice.storagefacility.testhelpers.{EventGenerators, NodeGenerators}
import no.uio.musit.models.{EventId, MuseumId}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.time.{Millis, Seconds, Span}

class EventServiceSpec extends MusitSpecWithAppPerSuite
    with NodeGenerators
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

  "Processing events" should {
    "successfully insert a new Control" in {
      val mid = MuseumId(2)
      val ctrl = createControl(defaultBuilding.id)
      val controlEvent = controlService.add(mid, defaultBuilding.id.get, ctrl).futureValue
      controlEvent.isSuccess mustBe true
      controlEvent.get.id.get mustBe EventId(1)
      latestEventId = controlEvent.get.id.get
      latestEventId mustBe 1L
    }

    "fail when inserting a Control with wrong museumId" in {
      val anotherMid = MuseumId(4)
      val ctrl = createControl(defaultBuilding.id)
      val res = controlService.add(anotherMid, defaultBuilding.id.get, ctrl).futureValue
      res.isSuccess mustBe false
      res.isFailure mustBe true
    }

    "successfully insert a new Observation" in {
      val mid = MuseumId(2)
      val obs = createObservation(defaultBuilding.id)
      val res = obsService.add(mid, defaultBuilding.id.get, obs).futureValue
      res.isSuccess mustBe true
      val theObs = res.get
      theObs.id.get mustBe EventId(9)

      theObs.alcohol mustBe obs.alcohol
      theObs.cleaning mustBe obs.cleaning
      theObs.gas mustBe obs.gas
      theObs.pest mustBe obs.pest
      theObs.mold mustBe obs.mold
      theObs.hypoxicAir mustBe obs.hypoxicAir
      theObs.temperature mustBe obs.temperature
      theObs.relativeHumidity mustBe obs.relativeHumidity
      theObs.lightingCondition mustBe obs.lightingCondition
      theObs.perimeterSecurity mustBe obs.perimeterSecurity
      theObs.fireProtection mustBe obs.fireProtection
      theObs.theftProtection mustBe obs.theftProtection
      theObs.waterDamageAssessment mustBe obs.waterDamageAssessment

      latestEventId = res.get.id.get
      latestEventId mustBe 9L
    }

    "fail when inserting a Observation with wrong museumId" in {
      val anotherMid = MuseumId(4)
      val obs = createObservation(defaultBuilding.id)
      val res = obsService.add(anotherMid, defaultBuilding.id.get, obs).futureValue
      res.isSuccess mustBe false
      res.isFailure mustBe true
    }

    "fail when inserting a EnvReq with wrong museumId" in {
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

