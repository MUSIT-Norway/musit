package repositories.loan.dao

import com.google.inject.{Inject, Singleton}
import models.loan.LoanEventTypes.{LentObjectsType, ReturnedObjectsType}
import models.loan.event.{LentObject, LoanEvent, ReturnedObject}
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import no.uio.musit.models.{EventId, MuseumId, ObjectUUID}
import no.uio.musit.time.dateTimeNow
import org.joda.time.DateTime
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class LoanDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends Tables {

  val logger = Logger(classOf[LoanDao])

  import driver.api._

  private def insertEventRow(event: LoanEventRow): DBIO[EventId] = {
    loanTable returning loanTable.map(_.id) += event
  }

  private def insertActiveLoanRow(activeLoanRow: ActiveLoanRow): DBIO[Long] = {
    activeLoanTable returning activeLoanTable.map(_.id) += activeLoanRow
  }

  private def insertActiveLoanRows(
      mid: MuseumId,
      eid: EventId,
      returnDate: DateTime,
      objs: Seq[ObjectUUID]
  ) = {
    val actions = objs.map(o => insertActiveLoanRow((None, mid, o, eid, returnDate)))
    DBIO.sequence(actions)
  }

  private def deleteActiveLoanRow(objectId: ObjectUUID): DBIO[Int] = {
    activeLoanTable.filter(_.objectUuid === objectId).delete
  }

  private def deleteActiveLoanRows(objectIds: Seq[ObjectUUID]): DBIO[Int] = {
    DBIO.sequence(objectIds.map(deleteActiveLoanRow)).map(_.sum)
  }

  private def insertLentObject(eId: EventId, obj: ObjectUUID): DBIO[Long] = {
    lentObjectTable returning lentObjectTable.map(_.id) += ((None, eId, obj))
  }

  private def insertLentObjects(eid: EventId, objs: Seq[ObjectUUID]) = {
    val actions = objs.map(o => insertLentObject(eid, o))
    DBIO.sequence(actions)
  }

  def insertReturnedObjectEvent(
      mid: MuseumId,
      retEvt: ReturnedObject
  ): Future[MusitResult[EventId]] = {
    val actions = for {
      _  <- deleteActiveLoanRows(retEvt.objects)
      id <- insertEventRow(asEventRowTuple(retEvt))
    } yield id
    db.run(actions.transactionally).map(MusitSuccess.apply).recover {
      case NonFatal(t) =>
        val msg = "Unable to insert returned loan"
        logger.warn(msg, t)
        MusitDbError(msg, Some(t))
    }
  }

  def insertLentObjectEvent(
      mid: MuseumId,
      lentObject: LentObject
  ): Future[MusitResult[EventId]] = {
    val actions = for {
      id <- insertEventRow(asEventRowTuple(lentObject))
      _  <- insertLentObjects(id, lentObject.objects)
      _  <- insertActiveLoanRows(mid, id, lentObject.returnDate, lentObject.objects)
    } yield id
    db.run(actions.transactionally).map(MusitSuccess.apply).recover {
      case NonFatal(t) =>
        val msg = "Unable to insert loan"
        logger.warn(msg, t)
        MusitDbError(msg, Some(t))
    }
  }

  def findExpectedReturnedObjects(
      mid: MuseumId
  ): Future[MusitResult[Seq[(ObjectUUID, DateTime)]]] = {
    val action = activeLoanTable.filter { r =>
      r.museumId === mid &&
      r.returnDate < dateTimeNow
    }.sortBy(_.returnDate).map(r => r.objectUuid -> r.returnDate)

    db.run(action.result).map(MusitSuccess.apply).recover {
      case NonFatal(t) =>
        val msg = "Unable to fetch active loans"
        logger.warn(msg, t)
        MusitDbError(msg, Some(t))
    }
  }

  def findEventForObject(objectUUID: ObjectUUID): Future[MusitResult[Seq[LoanEvent]]] = {
    val query = loanTable
      .join(lentObjectTable)
      .filter {
        case (lt, lo) => lt.objectUuid === objectUUID || lo.objectUuid === objectUUID
      }
      .sortBy(_._1.eventDate)
      .map(res => res._1.typeId -> res._1.eventJson)

    db.run(query.result)
      .map { res =>
        res.map { evt =>
          evt._1 match {
            case LentObjectsType     => evt._2.as[LentObject]
            case ReturnedObjectsType => evt._2.as[ReturnedObject]
          }
        }
      }
      .map(MusitSuccess.apply)
  }
}
