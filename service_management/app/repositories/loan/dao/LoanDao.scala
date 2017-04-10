package repositories.loan.dao

import com.google.inject.{Inject, Singleton}
import models.loan.LoanEventTypes.{ObjectLentType, ObjectReturnedType}
import models.loan.LoanType
import models.loan.event.{LoanEvent, ObjectsLent, ObjectsReturned}
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

  import profile.api._

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
      retEvt: ObjectsReturned
  ): Future[MusitResult[EventId]] = {
    val actions = for {
      _  <- deleteActiveLoanRows(retEvt.objects)
      id <- insertEventRow(asEventRowTuple(mid, retEvt))
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
      objectsLent: ObjectsLent
  ): Future[MusitResult[EventId]] = {
    val actions = for {
      id <- insertEventRow(asEventRowTuple(mid, objectsLent))
      _  <- insertLentObjects(id, objectsLent.objects)
      _  <- insertActiveLoanRows(mid, id, objectsLent.returnDate, objectsLent.objects)
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
        res.map {
          case (typ, json) =>
            typ match {
              case ObjectLentType     => json.as[ObjectsLent]
              case ObjectReturnedType => json.as[ObjectsReturned]
            }
        }
      }
      .map(MusitSuccess.apply)
  }

  private def activeLoan(mid: MuseumId) =
    activeLoanTable.filter(_.museumId === mid).map(_.eventId).distinct

  def findActiveLoanEvents(mid: MuseumId): Future[MusitResult[Seq[LoanEvent]]] = {
    val query = loanTable
      .filter(_.id in activeLoan(mid))
      .filter(_.typeId === ObjectLentType.asInstanceOf[LoanType])
      .sortBy(_.registeredDate.asc)

    db.run(query.result).map(r => r.map(fromLoanEventRow)).map(MusitSuccess.apply)
  }

}
