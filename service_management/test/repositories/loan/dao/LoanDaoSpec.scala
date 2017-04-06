package repositories.loan.dao

import models.loan.LoanEventTypes.{LentObjectsType, ReturnedObjectsType}
import models.loan.event.{LentObject, ReturnedObject}
import no.uio.musit.MusitResults.MusitSuccess
import no.uio.musit.models.{ActorId, MuseumId, ObjectUUID}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import no.uio.musit.time.dateTimeNow

class LoanDaoSpec extends MusitSpecWithAppPerSuite with MusitResultValues {

  val loanDao = fromInstanceCache[LoanDao]

  def lentObject() = LentObject(
    id = None,
    loanType = LentObjectsType,
    eventDate = Some(dateTimeNow),
    registeredBy = Some(ActorId.generate()),
    registeredDate = Some(dateTimeNow),
    partOf = None,
    note = None,
    returnDate = dateTimeNow.plusDays(10),
    objects = Seq(ObjectUUID.generate())
  )

  def retObject() = ReturnedObject(
    id = None,
    loanType = ReturnedObjectsType,
    eventDate = Some(dateTimeNow),
    registeredBy = Some(ActorId.generate()),
    registeredDate = Some(dateTimeNow),
    partOf = None,
    note = None,
    returnDate = dateTimeNow,
    objects = Seq(ObjectUUID.generate())
  )

  val mid = MuseumId(99)

  "LoanDao" when {

    "receives lent object event" should {

      "insert loan event" in {
        val res = loanDao.insertLentObjectEvent(mid, lentObject()).futureValue

        res mustBe a[MusitSuccess[_]]
      }

      "find loans that should have been returned" in {
        val evt = lentObject().copy(returnDate = dateTimeNow.minusDays(1))
        loanDao.insertLentObjectEvent(mid, evt).futureValue

        val res = loanDao.findExpectedReturnedObjects(mid).futureValue

        res.successValue.map(_._1) must contain theSameElementsAs evt.objects
      }
    }

    "receives returned object event" should {
      "insert returned event" in {
        val lentObj = lentObject()
        val retObj  = retObject().copy(objects = lentObj.objects)

        loanDao.insertLentObjectEvent(mid, lentObj).futureValue
        val res = loanDao.insertReturnedObjectEvent(mid, retObj).futureValue

        res mustBe a[MusitSuccess[_]]
      }

      "insert returned event when no active loan is present" in {
        val retObj = retObject()
        val res    = loanDao.insertReturnedObjectEvent(mid, retObj).futureValue

        res mustBe a[MusitSuccess[_]]
      }

      "remove object from active loans" in {
        val lentObj = lentObject()
        val retObj  = retObject().copy(objects = lentObj.objects)

        loanDao.insertLentObjectEvent(mid, lentObj).futureValue
        loanDao.insertReturnedObjectEvent(mid, retObj).futureValue

        val res = loanDao.findExpectedReturnedObjects(mid).futureValue

        res.successValue.map(_._1) must not contain theSameElementsAs(lentObj.objects)
      }
    }

    "find events " should {
      "related to object" in {
        val lentObj = lentObject()
        val retObj  = retObject().copy(objects = lentObj.objects)

        loanDao.insertLentObjectEvent(mid, lentObj).futureValue
        loanDao.insertReturnedObjectEvent(mid, retObj).futureValue
        val events     = loanDao.findEventForObject(lentObj.objects.head).futureValue
        val eventTypes = events.successValue.map(_.getClass)

        eventTypes must contain allOf (classOf[LentObject], classOf[ReturnedObject])
      }
    }

  }

}
