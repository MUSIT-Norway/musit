package repositories.actor.dao

import com.google.inject.Inject
import no.uio.musit.models.ActorId
import no.uio.musit.security.{AuthTables, UserInfo}
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class UserInfoDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
    with AuthTables {

  val logger = Logger(classOf[UserInfoDao])

  import profile.api._

  def getById(id: ActorId): Future[Option[UserInfo]] = {
    db.run(usrInfoTable.filter(_.uuid === id).result.headOption)
      .map { musr =>
        musr.map(userInfoFromTuple)
      }
      .recover {
        case NonFatal(ex) =>
          logger.error(s"An error occurred reading UserInfo for $id", ex)
          None
      }
  }

  def listBy(ids: Set[ActorId]): Future[Seq[UserInfo]] = {
    val query = usrInfoTable.filter(_.uuid inSet ids).sortBy(_.name)
    db.run(query.result.map(_.map(userInfoFromTuple)))
  }.recover {
    case NonFatal(ex) =>
      logger.error(s"An error occurred reading UserInfo for ${ids.mkString(", ")}", ex)
      Seq.empty
  }

  def getByName(searchString: String): Future[Seq[UserInfo]] = {
    val likeArg = searchString.toLowerCase
    val query   = usrInfoTable.filter(_.name.toLowerCase like s"%$likeArg%").sortBy(_.name)
    db.run(query.result.map(_.map(userInfoFromTuple))).recover {
      case NonFatal(ex) =>
        logger.error(s"An error occurred searching for UserInfo with $searchString", ex)
        Seq.empty
    }
  }

}
