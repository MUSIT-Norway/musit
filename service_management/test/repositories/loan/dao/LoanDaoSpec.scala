package repositories.loan.dao

import models.loan.LoanEventTypes.{ObjectLentType, ObjectReturnedType}
import models.loan.event.{ObjectsLent, ObjectsReturned}
import no.uio.musit.MusitResults.MusitSuccess
import no.uio.musit.models.{ActorId, ExternalRef, MuseumId, ObjectUUID}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import no.uio.musit.time.dateTimeNow

class LoanDaoSpec extends MusitSpecWithAppPerSuite with MusitResultValues {

  val loanDao = fromInstanceCache[LoanDao]

  def objectsLent() = ObjectsLent(
    id = None,
    loanType = ObjectLentType,
    eventDate = Some(dateTimeNow),
    registeredBy = Some(ActorId.generate()),
    registeredDate = Some(dateTimeNow),
    partOf = None,
    note = None,
    returnDate = dateTimeNow.plusDays(10),
    objects = Seq(ObjectUUID.generate()),
    externalRef = Some(ExternalRef(Seq("ef-10")))
  )

  def objectsReturned() = ObjectsReturned(
    id = None,
    loanType = ObjectReturnedType,
    eventDate = Some(dateTimeNow),
    registeredBy = Some(ActorId.generate()),
    registeredDate = Some(dateTimeNow),
    partOf = None,
    note = None,
    returnDate = dateTimeNow,
    objects = Seq(ObjectUUID.generate()),
    externalRef = Some(ExternalRef(Seq("ef-11")))
  )

  val mid = MuseumId(99)

  "LoanDao" when {

    "receives lent object event" should {

      "insert loan event" in {
        val res = loanDao.insertLentObjectEvent(mid, objectsLent()).futureValue

        res mustBe a[MusitSuccess[_]]
      }

      "find loans that should have been returned" in {
        val evt = objectsLent().copy(returnDate = dateTimeNow.minusDays(1))
        loanDao.insertLentObjectEvent(mid, evt).futureValue

        val res = loanDao.findExpectedReturnedObjects(mid).futureValue

        res.successValue.map(_._1) must contain theSameElementsAs evt.objects
      }
    }

    "receives returned object event" should {
      "insert returned event" in {
        val lent = objectsLent()
        val ret  = objectsReturned().copy(objects = lent.objects)

        loanDao.insertLentObjectEvent(mid, lent).futureValue
        val res = loanDao.insertReturnedObjectEvent(mid, ret).futureValue

        res mustBe a[MusitSuccess[_]]
      }

      "insert returned event when no active loan is present" in {
        val ret = objectsReturned()
        val res = loanDao.insertReturnedObjectEvent(mid, ret).futureValue

        res mustBe a[MusitSuccess[_]]
      }

      "remove object from active loans" in {
        val lent = objectsLent()
        val ret  = objectsReturned().copy(objects = lent.objects)

        loanDao.insertLentObjectEvent(mid, lent).futureValue
        loanDao.insertReturnedObjectEvent(mid, ret).futureValue

        val res = loanDao.findExpectedReturnedObjects(mid).futureValue

        res.successValue.map(_._1) must not contain theSameElementsAs(lent.objects)
      }
    }

    "find events " should {
      "related to object" in {
        val lent = objectsLent()
        val ret  = objectsReturned().copy(objects = lent.objects)

        loanDao.insertLentObjectEvent(mid, lent).futureValue
        loanDao.insertReturnedObjectEvent(mid, ret).futureValue
        val events     = loanDao.findEventForObject(lent.objects.head).futureValue
        val eventTypes = events.successValue.map(_.getClass)

        eventTypes must contain allOf (classOf[ObjectsLent], classOf[ObjectsReturned])
      }
    }

  }

}
