package dao

import com.google.inject.Inject
import models.MusitThing
import models.dto.MusitThingDto
import no.uio.musit.service.MusitResults.{MusitDbError, MusitResult, MusitSuccess, MusitValidationError}
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import services.FutureMusitResults
import slick.lifted.CanBeQueryCondition

import scala.concurrent.Future

/**
 * Created by jarle on 06.10.16.
 */

sealed trait FieldValue

case class EmptyValue() extends FieldValue
case class LiteralValue(v: String) extends FieldValue
case class WildcardValue(v: String) extends FieldValue
case class InvalidValue(errorMessage: String) extends FieldValue

class ObjectSearchDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] {

  val logger = Logger(classOf[ObjectAggregationDao])

  import driver.api._

  private val musitThingTable = TableQuery[MusitThingTable]


  def testInsert(museumId: Int, mthing: MusitThing): Future[Long] = {
    val dto = MusitThingDto.fromDomain(museumId, mthing)

    val action = musitThingTable returning musitThingTable.map(_.id) += dto
    println(s"insert sql: ${action.statements}")
    db.run(action) //.map(_.get)
  }

  def classifyValue(rawValue: String): FieldValue = {
    //Since we build up a Slick Query object, we don't need to verify that the rawValue is "safe",
    // the database engine will validate the parameter value.

    if(rawValue.contains('%')) {
      InvalidValue("Illegal character in search value")
    }
    else if(rawValue.isEmpty) EmptyValue()
    else if(rawValue.contains('*')) {
      WildcardValue(rawValue.replace('*', '%'))
    }
    else
    {
      LiteralValue(rawValue)
    }
  }

  def fieldValueToMusitResult(fieldValue: FieldValue): MusitResult[FieldValue] = {
    fieldValue match {
      case InvalidValue(errorMessage) => MusitValidationError(errorMessage, None, None)
      case default => MusitSuccess(default)
    }
  }

  def classifyValueAsMusitResult(rawValue: String): MusitResult[FieldValue] = {
    fieldValueToMusitResult(classifyValue(rawValue))
  }

  type QMusitThingTable = Query[MusitThingTable, MusitThingTable#TableElementType, scala.Seq]

  private def maybeAddFilter[Q <: QMusitThingTable](q: Q, value: FieldValue, filterEqual: (Q,String) => Q, filterLike: (Q, String) => Q): Q = {

    value match {
      case EmptyValue() => q
      case LiteralValue(v: String) => filterEqual(q, v)
      case WildcardValue(v: String) => filterLike(q, v)
      case InvalidValue(errorMessage: String) =>
        assert(false, s"internal error, invalid value: $errorMessage"); q //Returning q is just to make the compiler happy
    }
  }

  private def maybeAddMuseumNoFilter(q: QMusitThingTable, value: FieldValue): QMusitThingTable= {
    def isAllDigits(x: String) = x.forall(Character.isDigit)



    value match {
      case EmptyValue() => q
      case LiteralValue(v: String) =>
        if(isAllDigits(v)) {
          q.filter(_.museumNoAsNumber===v.toLong)
        }
        else {
          q.filter(_.museumNo.toUpperCase === v.toUpperCase)
        }
      case WildcardValue(v: String) => q.filter(_.museumNo.toUpperCase like v.toUpperCase)
      case InvalidValue(errorMessage: String) =>
        assert(false, s"internal error, invalid museumNo value: $errorMessage"); q //Returning q is just to make the compiler happy
    }
  }


  def search(mid: Int, museumNo: String, subNo: String, term: String, page: Int, pageSize: Int): Future[MusitResult[Seq[MusitThing]]] = {

    val triple =
      for {
        museumNoValue <- classifyValueAsMusitResult(museumNo)
        subNoValue <- classifyValueAsMusitResult(subNo)
        termValue <- classifyValueAsMusitResult(term)
      } yield (museumNoValue, subNoValue, termValue)

    val resultFutSeq = triple.map {
      case (musemNoValue, subNoValue, termValue) =>
        val q1 = maybeAddMuseumNoFilter(musitThingTable, musemNoValue)

        val q2 = maybeAddFilter[QMusitThingTable](q1, subNoValue, (q, value) => q.filter(_.subNo === value),
                                                                        (q,value) => q.filter(_.subNo like value))

        val q3 = maybeAddFilter[QMusitThingTable](q2, termValue, (q, value) => q.filter(_.term.toLowerCase === value.toLowerCase),
                                                                      (q, value) => q.filter(_.term.toLowerCase like value.toLowerCase))

        val query = q3
        val sortedQuery = query.sortBy(mt=> (mt.museumNoAsNumber.asc, mt.subNoAsNumber.asc, mt.subNo.asc, mt.id.asc))

        val offset = (page - 1) * pageSize
        val action = sortedQuery.drop(offset).take(pageSize).result
        println(s"SQL: ${action.statements}")
        val res = db.run(action)
          res.recover {
          case e: Exception =>
            val msg = s"Error while retrieving search result"
            logger.error(msg, e)
            MusitDbError(msg, Some(e))
        }
        res.map(_.map(MusitThingDto.toDomain(_)))
    }
    FutureMusitResults.invertMF(resultFutSeq)
  }



  class MusitThingTable(
      val tag: Tag
  ) extends Table[MusitThingDto](tag, Some("MUSIT_MAPPING"), "MUSITTHING") {

    def * = (museumId, id.?, museumNo, museumNoAsNumber, subNo, subNoAsNumber, term) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    val museumId = column[Int]("MUSEUMID")
    val museumNo = column[String]("MUSEUMNO")
    val subNo = column[Option[String]]("SUBNO")
    val term = column[String]("TERM")
    val museumNoAsNumber = column[Option[Long]]("MUSEUMNOASNUMBER")
    val subNoAsNumber = column[Option[Long]]("SUBNOASNUMBER")

    def create = (museumId: Int, id: Option[Long], museumNo: String, museumNoAsNumber: Option[Long],
                  subNo: Option[String], subNoAsNumber: Option[Long], term: String) =>
      MusitThingDto(
        museumId = museumId,
        id = id,
        museumNo = museumNo,
        museumNoAsNumber = museumNoAsNumber,
        subNo = subNo,
        subNoAsNumber = subNoAsNumber,
        term = term
      )

    def destroy(thing: MusitThingDto) =
      Some((thing.museumId, thing.id, thing.museumNo, thing.museumNoAsNumber, thing.subNo, thing.subNoAsNumber, thing.term))
  }
}
