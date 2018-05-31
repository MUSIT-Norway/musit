package repositories.musitsearchobject.dao

import com.google.inject.Inject
import models.musitsearchobject.SearchObjectResult
import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models.MuseumCollections._
import no.uio.musit.models._
import no.uio.musit.repositories.{BaseColumnTypeMappers, DbErrorHandlers}
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import org.joda.time.DateTime
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.Json
import repositories.conservation.DaoUtils
import repositories.musitobject.dao.ObjectTables
import repositories.musitobject.dao.SearchFieldValues.{
  EmptyValue,
  FieldValue,
  LiteralValue,
  WildcardValue
}
import repositories.shared.dao.{ColumnTypeMappers, SharedTables}
import slick.jdbc.{ResultSetConcurrency, ResultSetType}
import slick.sql.SqlAction

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

class MusitSearchObjectDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext,
    val daoUtils: DaoUtils
) extends ObjectTables
    with SharedTables
    with DbErrorHandlers
    with ColumnTypeMappers
    with BaseColumnTypeMappers
    with WithDateTimeFormatters {

  import profile.api._

  val schemaName                          = "MUSARK_THING"
  val searchObjectTableName               = "MUSITTHING_SEARCH"
  val schemaPrefixedSearchObjectTableName = s"$schemaName.$searchObjectTableName"
  val searchObjectPopulatingTableName     = "MUSITTHING_SEARCH_POPULATING"
  val schemaPrefixSearchObjectPopulatingTableName =
    s"$schemaName.$searchObjectPopulatingTableName"

  val searchObjectTable           = TableQuery[SearchObjectTable]
  val searchObjectPopulatingTable = TableQuery[SearchObjectTablePopulating]
  val test                        = searchObjectPopulatingTable.asInstanceOf[TableQuery[SearchObjectTable]]

  private val logger = Logger(classOf[MusitSearchObjectDao])

  type QSearchObjectTable =
    slick.lifted.Query[SearchObjectTable, SearchObjectTable#TableElementType, scala.Seq]

  // Needs to be the same as Slicks no-escape-char value!
  // (Default second parameter value to the like function)
  val noEscapeChar = '\u0000'

  // Can be any char really.
  val escapeChar = '¤'

  /*Helper classes for search in db*/

  case class LiftedSearchObjectRow(
      //  id: ObjectId,
      uuid: Rep[ObjectUUID],
      museumId: Rep[MuseumId],
      museumNo: Rep[String],
      subNo: Rep[Option[String]],
      term: Rep[String],
      //TODO?      museumNo_Prefix: Rep[Option[String]],
      collection: Rep[Option[Collection]],
      museumNo_Number: Rep[Option[Long]], //Long instead of Int because it is Int in objTable
      subNo_Number: Rep[Option[Long]], //Long instead of Int because it is Int in objTable
      document_json: Rep[Option[String]],
      updatedDate: Rep[DateTime]
  )

  case class SearchObjectRow(
      //  id: ObjectId,
      uuid: ObjectUUID,
      museumId: MuseumId,
      museumNo: String,
      subNo: Option[String],
      term: String,
      //TODO?      museumNo_Prefix: Option[String],
      collection: Option[Collection],
      museumNo_Number: Option[Long], //Long instead of Int because it is Int in objTable
      subNo_Number: Option[Long], //Long instead of Int because it is Int in objTable
      document_json: Option[String],
      updatedDate: DateTime
  ) {
    def toResultObject() = {
      SearchObjectResult(this.uuid, this.museumId, this.museumNo, this.subNo, this.term)
    }

  }

  implicit object SearchObjectRowShape
      extends CaseClassShape(LiftedSearchObjectRow.tupled, SearchObjectRow.tupled)

  val writes = Json.writes[SearchObjectRow]

  trait CommonStuff extends Table[SearchObjectRow] {
    val objectuuid = column[ObjectUUID]("OBJECTUUID", O.PrimaryKey)
    val museumId   = column[MuseumId]("MUSEUMID")
    val museumNo   = column[String]("MUSEUMNO")
    val subNo      = column[Option[String]]("SUBNO")
    //    val museumNo   = column[MuseumNo]("museumNo")
    //    val subNo      = column[Option[SubNo]]("subNo")
    val term            = column[String]("TERM")
    val updatedDate     = column[DateTime]("UPDATED_DATE")
    val collection      = column[Option[Collection]]("NEW_COLLECTION_ID")
    val museumNo_Number = column[Option[Long]]("MUSEUMNO_NUMBER") //Long instead of Int because it is Int in objTable
    val subNo_Number    = column[Option[Long]]("SUBNO_NUMBER") //--||--
    val document_json   = column[Option[String]]("DOCUMENT_JSON")

    def * =
      LiftedSearchObjectRow(
        objectuuid,
        museumId,
        museumNo,
        subNo,
        term,
        collection,
        museumNo_Number,
        subNo_Number,
        document_json,
        updatedDate
      )
  }

  /**
   * Dao intended for searching through objects
   */
  class SearchObjectTable(
      val tag: Tag,
      val useTableName: String = searchObjectTableName
  ) extends Table[SearchObjectRow](
        tag,
        Some(schemaName),
        useTableName
      )
      with CommonStuff {

    val logger = Logger(classOf[SearchObjectTable])

  }

  class SearchObjectTablePopulating(
      override val tag: Tag
  ) extends SearchObjectTable(tag, searchObjectPopulatingTableName)
      with CommonStuff {}

  protected def dbRunAndLogProblems[R](ac: DBIO[R], statement: => String): Future[R] = {
    def logProblems[T](fut: Future[T])(implicit ec: ExecutionContext): Future[T] = {
      fut.onFailure {
        case ex =>
          logger.error(
            s"Slick problem when executing statement: ${statement}" + ex.getMessage
          )
      }
      fut
    }

    logProblems(db.run(ac))
  }

  def updateJsonColumn(
      table: slick.lifted.TableQuery[SearchObjectTable],
      id: ObjectUUID
  )(
      implicit ec: ExecutionContext
  ): Future[Int] = {
    val ac                = table.filter(_.objectuuid === id).result.headOption
    val futOptExistingRow = dbRunAndLogProblems(ac, "get row to update document_json")
    futOptExistingRow.flatMap {
      case Some(row) =>
        val rowWithBlankDocumentJson = row.copy(document_json = None)

        val rowAsJson = writes.writes(rowWithBlankDocumentJson).toString()

        val updateAc =
          table.filter(_.objectuuid === id).map(_.document_json).update(Some(rowAsJson))

        val res = dbRunAndLogProblems(updateAc, s"update document_json for row $id")
        res

      case None =>
        Future.successful(0)
    }
  }
  /*
  def updateJsonColumn(
      table: slick.lifted.TableQuery[SearchObjectTable],
      id: ObjectUUID
  )(
      implicit ec: ExecutionContext
  ): DBIO[Int] = {

    for {
      existing <- table.filter(_.objectuuid === id).result.headOption
      row             = existing.get.copy(document_json = None)
      rowAsJsonString = writes.writes(row).toString()
      result <- table
                 .filter(_.objectuuid === id)
                 .map(_.document_json)
                 .update(Some(rowAsJsonString))
    } yield result
}
   */

  private def setCalculatedValuesForAllRowsInQueryResult(
      table: slick.lifted.TableQuery[SearchObjectTable],
      q: Query[Rep[ObjectUUID], ObjectUUID, scala.Seq]
  )(implicit ec: ExecutionContext): Future[Unit] = {
    var rowNum     = 0
    var errorCount = 0
    val qResult = q.result
      .withStatementParameters(
        rsType = ResultSetType.ForwardOnly,
        rsConcurrency = ResultSetConcurrency.ReadOnly,
        fetchSize = 128
      )
      .transactionally
    logger.info("setCalculatedValuesForAllRowsInQueryResult-SQL:" + q.result.statements)
    val res = db.stream(qResult).foreach { r =>
      val futRes = updateJsonColumn(table, r)
      //val fut = dbRunAndLogProblems(ac, s"update for $r")
      val res = Await.ready(futRes, 10 minutes) //I think we need to do await here, because after the db.stream.foreach, we rename the table, so these must be completed by then.
      res.value.map { x =>
        x match {
          case Success(_) => ()
          case Failure(s) =>
            errorCount = errorCount + 1
            logger.error(
              s"setCalculatedValuesForAllRowsInQueryResult, problem with updating row $r: ${s.getMessage}"
            )

        }
        rowNum = rowNum + 1
        if (rowNum % 1000 == 0) {
          logger.info(s"setCalculatedValuesForAllRowsInQueryResult -- RowNum: $rowNum")
        }

      }

    }
    logger.info(s"---- Rows attempted updated: ${rowNum} Errors: $errorCount")
    res
  }
  def updateSearchTable(afterDate: DateTime)(implicit ec: ExecutionContext) = {

    logger.info(s"updateSearchTable afterDate=$afterDate")

    val updatedRows = objTable.filter(_.updatedDate > afterDate)

    val rowsToDelete =
      searchObjectTable.filter(so => so.objectuuid in (updatedRows.map(t => t.uuid)))

    val deleteAction = rowsToDelete.delete

    logger.info("updateSearchTable: delete sql: " + deleteAction.statements)

    val updatedDate = dateTimeNow

    val nonDeletedUpdatedRows = updatedRows.filterNot(_.isDeleted)

    val insertAction = searchObjectTable.forceInsertQuery(nonDeletedUpdatedRows.map { t =>
      LiftedSearchObjectRow(
        t.uuid.get,
        t.museumId,
        t.museumNo,
        t.subNo,
        t.term,
        t.newCollectionId,
        t.museumNoAsNumber,
        t.subNoAsNumber,
        None,
        updatedDate
      )
    })
    logger.info("updateSearchTable insert sql: " + insertAction.statements)
    //val ac:JdbcProfile.this.ProfileAction[Int, NoStream, Effect.Write] = insertAction
    val ac   = insertAction
    val test = ac.statements

    dbRunAndLogProblems(
      DBIO.seq(deleteAction, insertAction),
      s"when deleting and inserting into ${schemaPrefixedSearchObjectTableName}" /*, actions*/
    )
    //Note that we assume the objTable.uuid columns is not null (in practice, even though at the moment there's no explicit constraint)

    val nonDeletedUuidsQ = nonDeletedUpdatedRows.map(r => r.uuid.get)
    setCalculatedValuesForAllRowsInQueryResult(searchObjectTable, nonDeletedUuidsQ)
  }

  private def renameTables(): Future[Unit] = {
    logger.info("Renaming tables")
    val tempTableName               = "TempTable1"
    val schemaPrefixedTempTableName = s"$schemaName.$tempTableName"

    val actions = Seq(
      sqlu"alter table #${schemaPrefixSearchObjectPopulatingTableName} rename to #${tempTableName}",
      sqlu"alter table #${schemaPrefixedSearchObjectTableName} rename to #${searchObjectPopulatingTableName}",
      sqlu"alter table #${schemaPrefixedTempTableName} rename to #${searchObjectTableName}",
      sqlu"truncate table #${schemaPrefixSearchObjectPopulatingTableName}"
    )
    actions.foreach(ac => logger.info(ac.statements.toString()))

    dbRunAndLogProblems(DBIO.sequence(actions), "alter table stuff...")
      .map(_ => ()) //The last map is there just to ignore the result, mapping it to Unit

  }

  def recreateSearchTable()(implicit ec: ExecutionContext) = {
    logger.info("RecreateSearchTable...")
    val tableToWorkWith =
      searchObjectPopulatingTable.asInstanceOf[TableQuery[SearchObjectTable]]
    //    .asInstanceOf[slick.lifted.TableQuery[MusitSearchObjectDao.this.SearchObjectTable]]

    val deleteAction = tableToWorkWith.delete

    val updatedDate = dateTimeNow

    val nonDeletedSourceRows = objTable.filterNot(_.isDeleted)

    val insertAction = tableToWorkWith.forceInsertQuery(nonDeletedSourceRows.map { t =>
      LiftedSearchObjectRow(
        t.uuid.get,
        t.museumId,
        t.museumNo,
        t.subNo,
        t.term,
        t.newCollectionId,
        t.museumNoAsNumber,
        t.subNoAsNumber,
        None,
        updatedDate
      )
    })

    /*
    val insertAction = sqlu(
      """insert into MUSARK_THING.MUSITTHING_SEARCH_POPULATING (OBJECTUUID,MUSEUMID, MUSEUMNO, SUBNO,TERM,NEW_COLLECTION_ID,MUSEUMNO_NUMBER,
      SUBNO_NUMBER,DOCUMENT_JSON,UPDATED_DATE)
    select MUSITTHING_UUID, MUSEUMID, MUSEUMNO, SUBNO, TERM, NEW_COLLECTION_ID, MUSEUMNOASNUMBER, SUBNOASNUMBER, null, {ts '2018-05-23 11:45:53.564'} from "MUSIT_MAPPING"."MUSITTHING" where not ("IS_DELETED" = 1)
    """
    )
     */

    logger.info("recreateSearchTable, delete sql: " + deleteAction.statements)
    logger.info("recreateSearchTable, insert sql: " + insertAction.statements)
    dbRunAndLogProblems(
      DBIO.seq(deleteAction, insertAction),
      s"delete: ${deleteAction.statements} insert: ${insertAction.statements}"
    )

    //Note that we assume the objTable.uuid columns is not null (in practice, even though at the moment there's no explicit constraint)
    //val nonDeletedUuidsQ = nonDeletedSourceRows.map(r => r.uuid.get)
    val uuidsToUpdate = tableToWorkWith.map(r => r.objectuuid)

    setCalculatedValuesForAllRowsInQueryResult(tableToWorkWith, uuidsToUpdate).flatMap(
      _ => renameTables()
    )

  }

  /* Searching. Some of this code is copied from the old db search code. */

  /**
   * Since we build up a Slick Query object, we don't need to verify that the
   * rawValue is "safe", the database engine will validate the parameter value.
   * So security-wise, we don't need to guard against '--' etc.
   * We use '*' as wildcard symbol. And treat both '%' and '_' as ordinary
   * characters, both in like-tests and equality tests.
   *
   * @param rawValue and Option[String] with the value to classify
   * @return A classified instance of FieldValue
   */
  private[dao] def classifyValue(rawValue: Option[String]): Option[FieldValue] = {
    rawValue.map { raw =>
      if (raw.isEmpty) {
        EmptyValue()
      } else if (raw.contains('*')) {
        // Note that in the below expression, order is vital! It is essential that
        // the escapeChar -> escapeChar+escapeChar is done before the replacements
        // which introduces any escapeChars and that %->escapeChar happens
        // before *->'%'
        val wValue = raw
          .replace(escapeChar.toString, s"$escapeChar$escapeChar")
          .replace("%", s"$escapeChar%")
          .replace("_", s"${escapeChar}_")
          .replace('*', '%')

        val esc = if (wValue.contains(escapeChar)) escapeChar else noEscapeChar
        WildcardValue(wValue, esc)
      } else {
        LiteralValue(raw)
      }
    }
  }

  private def subNoFilter[Q <: QSearchObjectTable, C](
      q: Q,
      v: FieldValue
  ): QSearchObjectTable = {
    v match {
      case EmptyValue() =>
        logger.debug("Using empty value for subNo filter")
        q //No need to transform the query.
      case LiteralValue(value) =>
        logger.debug("Using literal value for subNo filter")
        q.filter(_.subNo.toUpperCase === value.toUpperCase)

      case WildcardValue(value, esc) =>
        logger.debug("Using wildcard value for subNo filter")
        q.filter(_.subNo.toUpperCase like (value.toUpperCase, esc))
    }
  }

  private def termFilter[Q <: QSearchObjectTable, C](
      q: Q,
      v: FieldValue
  ): QSearchObjectTable = {
    v match {
      // No value to search for means we don't append a filter.
      case EmptyValue() =>
        logger.debug("Using empty value for term filter")
        q

      case LiteralValue(value) =>
        logger.debug("Using literal value for term filter")
        q.filter(_.term.toUpperCase === value.toUpperCase)

      case WildcardValue(value, esc) =>
        logger.debug("Using wildcard value for term filter")
        q.filter(_.term.toUpperCase like (value.toUpperCase, esc))
    }
  }

  private def museumNoFilter(q: QSearchObjectTable, v: FieldValue): QSearchObjectTable = {
    v match {
      case EmptyValue() =>
        logger.debug("Using empty value for museumNo filter")
        q

      case LiteralValue(value) =>
        logger.debug("Using literal value for museumNo filter")
        val digitsOnly = value.forall(Character.isDigit)
        if (digitsOnly) q.filter(_.museumNo_Number === value.toLong)
        else q.filter(_.museumNo.toUpperCase === value.toUpperCase)

      case WildcardValue(value, esc) =>
        logger.debug("Using wildcard value for museumNo filter")
        q.filter(_.museumNo.toUpperCase like (value.toUpperCase, esc))
    }
  }

  private[dao] def searchQueryWithoutSorting(
      mid: MuseumId,
      museumNo: Option[MuseumNo],
      subNo: Option[SubNo],
      term: Option[String],
      collections: Seq[MuseumCollection]
  )(implicit currUsr: AuthenticatedUser): QSearchObjectTable = {
    logger.debug(s"Performing search in collections: ${collections.mkString(", ")}")

    val mno = museumNo.map(_.value)

    val q1 = classifyValue(mno)
      .map(
        f => museumNoFilter(searchObjectTable, f)
      )
      .getOrElse(searchObjectTable)
    val q2 = classifyValue(subNo.map(_.value)).map(f => subNoFilter(q1, f)).getOrElse(q1)
    val q3 = classifyValue(term).map(f => termFilter(q2, f)).getOrElse(q2)
    val q4 = q3.filter(_.museumId === mid)
    if (currUsr.hasGodMode) q4
    // Filter on collection access if the user doesn't have GodMode
    else q4.filter(_.collection inSet collections.map(_.collection).distinct)

  }

  private[dao] def addSorting(
      q: QSearchObjectTable
  )(implicit currUsr: AuthenticatedUser): QSearchObjectTable = {

    // Tweak here if sorting needs to be tuned
    q /*.filter(_.isDeleted === false)*/ .sortBy { mt =>
      (
        mt.museumNo_Number.asc,
        mt.museumNo.toLowerCase.asc,
        mt.subNo_Number.asc,
        mt.subNo.toLowerCase.asc
      )
    }

  }

  /** pageNr starts at 1 **/
  private[dao] def paging(
      q: QSearchObjectTable,
      pageNr: Int,
      pageSize: Int
  ): QSearchObjectTable = {

    assert(pageNr >= 1)
    assert(pageSize >= 0)
    val toDrop = (pageNr - 1) * pageSize
    q.drop(toDrop).take(pageSize)
  }

  /** pageNr starts at 1 **/
  def runAndCreatePagedResult[T](
      qWithoutSorting: QSearchObjectTable,
      qWithSearching: QSearchObjectTable,
      page: Int,
      pageSize: Int,
      createRow: SearchObjectRow => T
  ): FutureMusitResult[PagedResult[T]] = {
    logger.debug("count query: " + qWithoutSorting.length.result.statements)

    val futCount = db.run(qWithoutSorting.length.result)
    val q1       = paging(qWithSearching, page, pageSize)
    logger.debug("result query: " + q1.result.statements)

    val futResult = daoUtils
      .dbRun(q1.result, "Unable to execute search (MusitSearchObjectDao.executeSearch)")
      .map(_.map(t => createRow(t)))

    for {
      count  <- FutureMusitResult.from(futCount)
      result <- futResult

    } yield PagedResult(count, result)

  }

  /** pageNr starts at 1 **/
  def executeSearch(
      mid: MuseumId,
      museumNo: Option[MuseumNo],
      subNo: Option[SubNo],
      term: Option[String],
      collections: Seq[MuseumCollection],
      page: Int,
      pageSize: Int
  )(
      implicit currUsr: AuthenticatedUser
  ): FutureMusitResult[PagedResult[SearchObjectResult]] = {

    val searchQ            = searchQueryWithoutSorting(mid, museumNo, subNo, term, collections)
    val searchQWithSorting = addSorting(searchQ)
    runAndCreatePagedResult(
      searchQ,
      searchQWithSorting,
      page,
      pageSize,
      t => t.toResultObject()
    )
  }
}
