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
    if(rawValue.isEmpty) EmptyValue() else {
      LiteralValue(rawValue) //Todo!
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

  def search(mid: Int, museumNo: String, subNo: String, term: String, page: Int, pageSize: Int): Future[MusitResult[Seq[MusitThing]]] = {
    println("i dao")
    var query = musitThingTable.filter(_.id > 0L) //Just to get correct type! TODO: How to get a Query from a TableQuery?


    def maybeAdd(value: FieldValue, filterEqual: String => QMusitThingTable, filterLike: String => QMusitThingTable) = {

      value match {
        case EmptyValue() => query
        case LiteralValue(v: String) => query = filterEqual(v)
        case WildcardValue(v: String) => query = filterLike(v)
        case InvalidValue(errorMessage: String) => assert(false, "internal error")

      }
    }

    val triple =
      for {
        museumNoValue <- classifyValueAsMusitResult(museumNo)
        subNoValue <- classifyValueAsMusitResult(subNo)
        termValue <- classifyValueAsMusitResult(term)
      } yield (museumNoValue, subNoValue, termValue)

    val resultFutSeq = triple.map {
      case (musemNoValue, subNoValue, termValue) =>
        maybeAdd(musemNoValue, value => query.filter(_.museumNo === value), value => query.filter(_.museumNo like value))
        maybeAdd(subNoValue, value => query.filter(_.subNo === value), value => query.filter(_.subNo like value))
        maybeAdd(termValue, value => query.filter(_.term === value), value => query.filter(_.term like value))

        val offset = (page - 1) * pageSize
        val action = query.result // .drop(offset).take(pageSize).result
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

    //    case class MusitThingDto(museumId: Int, id: Long, museumNo: String, museumNoAsNumber: Option[Long], subNo: String, term: String)

    def * = (museumId, id.?, museumNo, museumNoAsNumber, subNo, term) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    val museumId = column[Int]("MUSEUMID")
    val museumNo = column[String]("MUSEUMNO")
    val subNo = column[Option[String]]("SUBNO")
    val term = column[String]("TERM")
    val museumNoAsNumber = column[Option[Long]]("MUSEUMNOASNUMBER")

    def create = (museumId: Int, id: Option[Long], museumNo: String, museumNoAsNumber: Option[Long], subNo: Option[String], term: String) =>
      MusitThingDto(
        museumId = museumId,
        id = id,
        museumNo = museumNo,
        museumNoAsNumber = museumNoAsNumber,
        subNo = subNo,
        term = term
      )

    def destroy(thing: MusitThingDto) =
      Some((thing.museumId, thing.id, thing.museumNo, thing.museumNoAsNumber, thing.subNo, thing.term))
  }
}
