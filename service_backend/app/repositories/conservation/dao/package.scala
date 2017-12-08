package repositories.conservation

import com.google.inject.{Inject, Singleton}
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.repositories.DbErrorHandlers
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.{GetResult, JdbcProfile}

import scala.concurrent.{ExecutionContext, Future}

package object dao {
  val SchemaName                     = "MUSARK_CONSERVATION"
  val ConservationEventTableName     = "EVENT"
  val ConservationEventTypeTableName = "EVENT_TYPE"
  val ObjectEventTableName           = "OBJECT_EVENT"
  val MappingSchemaName              = "MUSIT_MAPPING"
  val TreatmentMaterialTableName     = "TREATMENT_MATERIAL"
  val TreatmentKeywordTableName      = "TREATMENT_KEYWORD"
  val EventActorsRolesTableName      = "EVENT_ACTOR_ROLE_DATE"
  val RolesTableName                 = "ROLE"
  val ConditionCodeTableName         = "CONDITION_CODE"
  val EventDocumentTableName         = "EVENT_DOCUMENT"
}
@Singleton
class DaoUtils @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
    with DbErrorHandlers {

  import profile.api._

  def dbRun[T](res: DBIO[T], onErrorMsg: String): FutureMusitResult[T] = {

    FutureMusitResult(
      db.run(res).map(MusitSuccess.apply).recover {
        nonFatal(onErrorMsg)
      }
    )
  }
}
