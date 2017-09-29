package repositories.auth.dao

import com.google.inject.{Inject, Singleton}
import models._
import no.uio.musit.MusitResults.{MusitDbError, MusitError, MusitResult, MusitSuccess}
import no.uio.musit.models.MuseumCollections.Collection
import no.uio.musit.models._
import no.uio.musit.security._
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class AuthDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext
) extends AuthTables {

  import profile.api._

  val logger = Logger(classOf[AuthDao])

  private def handleError(msg: String, ex: Throwable): MusitError = {
    logger.error(msg, ex)
    MusitDbError(msg, Some(ex))
  }

  def groupFromTuple(tuple: GroupDBTuple): Group = {
    Group(
      id = tuple._1,
      name = tuple._2,
      module = tuple._3,
      permission = tuple._4,
      museumId = tuple._5,
      description = tuple._6
    )
  }

  /**
   * method for adding a Group to the AUTH_GROUP table.
   *
   * @param g GroupAdd data to insert
   * @return The newly inserted Group complete with a GroupId.
   */
  def addGroup(g: GroupAdd): Future[MusitResult[Group]] = {
    val gid = GroupId.generate()
    val action = grpTable += (
      (
        gid,
        g.name,
        g.module,
        g.permission,
        g.museumId,
        g.description
      )
    )

    db.run(action).map(_ => MusitSuccess(Group.fromGroupAdd(gid, g))).recover {
      case NonFatal(ex) =>
        handleError(s"An error occurred inserting new Group ${g.name}", ex)
    }
  }

  def findUserDetails(feideEmail: Email): Future[MusitResult[Option[UserPermissions]]] = {
    val q = usrGrpTable.filter(_.feideEmail === feideEmail) joinLeft
      grpTable on (_.groupId === _.id) joinLeft
      usrInfoTable on ((t1, t2) => t2.secId === t1._1.feideEmail)

    db.run(q.result).map { rows =>
      val mud = rows.foldLeft[Option[UserPermissions]](None) { (userDetails, row) =>
        val grp    = groupFromTuple(row._1._2.get)
        val grpMem = row._1._1
        val usrInf = row._2.map(userInfoFromTuple)

        userDetails.map { u =>
          u.addPermission(
            mid = grp.museumId,
            module = grp.module,
            permission = grp.permission,
            collection = grpMem.collection.map(Collection.fromCollectionUUID)
          )
        }.orElse {
          Some(
            UserPermissions(
              feideEmail = grpMem.feideEmail,
              name = usrInf.flatMap(_.name),
              email = usrInf.flatMap(_.email),
              userId = usrInf.map(_.id)
            ).addPermission(
              mid = grp.museumId,
              module = grp.module,
              permission = grp.permission,
              collection = grpMem.collection.map(Collection.fromCollectionUUID)
            )
          )
        }
      }
      MusitSuccess(mud)
    }
  }

  def findUserDetailsForMuseum(
      mid: MuseumId
  ): Future[MusitResult[Map[ModuleConstraint, Seq[UserPermissions]]]] = {
    val q = grpTable.filter(_.museumId === mid) joinLeft
      usrGrpTable on (_.id === _.groupId) joinLeft
      usrInfoTable on ((t1, t2) => t2.secId === t1._2.map(_.feideEmail))

    db.run(q.result).map { rows =>
      val normalisedRows = rows.map(r => (r._1._1, r._1._2, r._2))
      val r              = accumulateUserDetails(normalisedRows)
      MusitSuccess(r)
    }
  }

  type UserDetailsQueryRow =
    (GroupDBTuple, Option[UserGroupMembership], Option[UserInfoDBTuple])

  private[this] def reduceToUserDetails(
      module: ModuleConstraint,
      moduleRows: Seq[UserDetailsQueryRow]
  ) = {
    moduleRows.foldLeft(Seq.empty[UserPermissions]) {
      case (usrs, row) =>
        val grp    = groupFromTuple(row._1)
        val usrInf = row._3.map(userInfoFromTuple)
        row._2.map { grpMem =>
          usrs.zipWithIndex
            .find(_._1.feideEmail == grpMem.feideEmail)
            .map {
              case (u, i) =>
                val usr = u.addPermission(
                  mid = grp.museumId,
                  module = module,
                  permission = grp.permission,
                  collection = grpMem.collection.map(Collection.fromCollectionUUID)
                )
                usrs.updated(i, usr)
            }
            .getOrElse {
              val ud = UserPermissions(
                feideEmail = grpMem.feideEmail,
                name = usrInf.flatMap(_.name),
                email = usrInf.flatMap(_.email),
                userId = usrInf.map(_.id)
              ).addPermission(
                mid = grp.museumId,
                module = module,
                permission = grp.permission,
                collection = grpMem.collection.map(Collection.fromCollectionUUID)
              )
              ud +: usrs
            }
        }.getOrElse(usrs)
    }
  }

  private[this] def accumulateUserDetails(rows: Seq[UserDetailsQueryRow]) = {
    rows
    // Group the rows by MuseumId
      .groupBy(_._1._5)
      // Fold over the rows to reduce them down to the desired return type.
      // The resulting map contains 1 collection of UserDetails per constraint.
      .foldLeft(Map.empty[ModuleConstraint, Seq[UserPermissions]]) {
        case (acc, (_, museumRows)) =>
          // For each MuseumId, group the rows by the constraint
          val groupedDetails = museumRows.groupBy(_._1._3).map {
            case (module, moduleRows) =>
              // For each modules, reduce down to a collection of UserDetails
              module -> reduceToUserDetails(module, moduleRows)
          }
          acc ++ groupedDetails
      }
  }

  /**
   * find GroupInfos for the given feide-email.
   *
   * @param feideEmail The feide email
   * @return A collection of GroupInfo belonging to the given feide-email
   */
  def findGroupInfoFor(feideEmail: Email): Future[MusitResult[Seq[GroupInfo]]] = {
    findGroupInfoBy(usrGrpTable.filter { ug =>
      ug.feideEmail.toLowerCase === feideEmail
    }).recover {
      case NonFatal(ex) =>
        handleError(s"An error occurred trying find GroupInfo for user $feideEmail", ex)
    }
  }

  def revokeGroup(
      feideEmail: Email,
      groupId: GroupId
  ): Future[MusitResult[Int]] = {
    val q = usrGrpTable.filter { ug =>
      ug.feideEmail.toLowerCase === feideEmail &&
      ug.groupId === groupId
    }.delete

    db.run(q).map {
      case res: Int if res == 1 =>
        MusitSuccess(res)

      case res: Int if res == 0 =>
        MusitSuccess(res)

      case res =>
        val msg = s"An unexpected number of groups ($res) where revoked from $feideEmail"
        logger.warn(msg)
        MusitDbError(msg)
    }
  }

  def revokeCollectionFor(
      feideEmail: Email,
      groupId: GroupId,
      collectionId: CollectionUUID
  ): Future[MusitResult[Int]] = {
    val q = usrGrpTable.filter { ug =>
      ug.feideEmail.toLowerCase === feideEmail &&
      ug.groupId === groupId &&
      ug.collectionId === collectionId
    }.delete

    db.run(q)
      .map {
        case res: Int if res == 1 =>
          logger.debug(
            s"Successfully revoked collection $collectionId " +
              s"for $feideEmail on $groupId"
          )
          MusitSuccess(res)

        case res: Int if res == 0 =>
          logger.debug(
            s"Collection $collectionId was not removed from $feideEmail " +
              s"on $groupId."
          )
          MusitSuccess(res)

        case res: Int =>
          val msg = s"An unexpected number of collections where removed from " +
            s"GroupId $groupId for $feideEmail"
          logger.warn(msg)
          MusitDbError(msg)

      }
      .recover {
        case NonFatal(ex) =>
          handleError(
            s"An error occurred when trying to delete the " +
              s"collection $collectionId for $feideEmail from $groupId",
            ex
          )
      }
  }

  /**
   * WARNING: This is a full table scan
   */
  def allGroups: Future[MusitResult[Seq[Group]]] = {
    val query = grpTable.sortBy(_.name)
    db.run(query.result)
      .map { grps =>
        MusitSuccess(grps.map {
          case (gid, name, module, perm, mid, desc) =>
            Group(gid, name, module, perm, mid, desc)
        })
      }
      .recover {
        case NonFatal(ex) =>
          handleError(s"An error occurred when trying to get all Groups", ex)
      }
  }

  def allGroupsFor(museumId: MuseumId): Future[MusitResult[Seq[Group]]] = {
    val query = grpTable.filter(_.museumId === museumId).sortBy(_.name)
    db.run(query.result)
      .map { grps =>
        MusitSuccess(grps.map {
          case (gid, name, module, perm, mid, desc) =>
            Group(gid, name, module, perm, mid, desc)
        })
      }
      .recover {
        case NonFatal(ex) =>
          handleError(s"An error occurred when trying to get all Groups", ex)
      }
  }

  def updateGroup(grp: Group): Future[MusitResult[Option[Group]]] = {
    val action = grpTable.filter { g =>
      g.id === grp.id
    }.update(
      (grp.id, grp.name, grp.module, grp.permission, grp.museumId, grp.description)
    )

    db.run(action)
      .map {
        case res: Int if res == 1 => MusitSuccess(Some(grp))
        case res: Int if res == 0 => MusitSuccess(None)
        case res: Int =>
          val msg = s"Wrong number of rows ($res) updated for group ${grp.id}"
          logger.warn(msg)
          MusitDbError(msg)
      }
      .recover {
        case NonFatal(ex) =>
          handleError(s"An error occurred when trying to update the Group ${grp.id}", ex)
      }
  }

  def deleteGroup(grpId: GroupId): Future[MusitResult[Int]] = {
    val action1 = usrGrpTable.filter(_.groupId === grpId).delete
    val action2 = grpTable.filter(_.id === grpId).delete

    // First we need to remove the users from the group.
    // Then we remove the group.
    val action = action1.andThen(action2).transactionally

    db.run(action)
      .map {
        case res: Int if res == 1 =>
          logger.debug(s"Successfully removed Group $grpId")
          MusitSuccess(res)

        case res: Int if res == 0 =>
          logger.debug(s"Group $grpId was not removed.")
          MusitSuccess(res)

        case res: Int =>
          val msg = s"An unexpected number of groups where removed using GroupId $grpId"
          logger.warn(msg)
          MusitDbError(msg)

      }
      .recover {
        case NonFatal(ex) =>
          handleError(s"An error occurred when trying to delete the Group $grpId", ex)
      }
  }

  def addUserToGroup(
      feideEmail: Email,
      grpId: GroupId,
      maybeCollections: Option[Seq[CollectionUUID]]
  ): Future[MusitResult[Unit]] = {
    val ugms = UserGroupMembership.applyMulti(feideEmail, grpId, maybeCollections)

    val action = (usrGrpTable ++= ugms).map(_.fold(1)(identity))

    db.run(action)
      .map { res =>
        logger.debug(s"Added user $feideEmail to group $grpId returned $res")
        MusitSuccess(())
      }
      .recover {
        case NonFatal(ex) =>
          handleError(
            s"An error occurred when inserting a new UserGroup relation " +
              s"between user $feideEmail and groupId $grpId",
            ex
          )
      }
  }

  def addUserToGroups(
      feideEmail: Email,
      accesses: Seq[AddAccess]
  ): Future[MusitResult[Unit]] = {
    val ugms = accesses.flatMap { a =>
      UserGroupMembership.applyMulti(feideEmail, a.groupId, a.collections)
    }.distinct
    val gids = ugms.map(_.groupId)

    val action = (usrGrpTable ++= ugms).map(_.fold(1)(identity))

    db.run(action)
      .map { res =>
        logger.debug(
          s"Added user $feideEmail to groups ${gids.mkString(", ")} returned $res"
        )
        MusitSuccess(())
      }
      .recover {
        case NonFatal(ex) =>
          handleError(
            s"An error occurred when inserting a new UserGroup relation for user" +
              s" $feideEmail and groupIds ${gids.mkString(", ")}",
            ex
          )
      }
  }

  def allCollections: Future[MusitResult[Seq[MuseumCollection]]] = {
    val query = musColTable.sortBy(_.name)
    db.run(query.result)
      .map { cols =>
        MusitSuccess(cols.map {
          case (uuid, name, oldSchemas) => MuseumCollection(uuid, name, oldSchemas)
        })
      }
      .recover {
        case NonFatal(ex) =>
          handleError(s"An error occurred when trying to get all MuseumCollections.", ex)
      }
  }

  def allUsers: Future[MusitResult[Seq[RegisteredUser]]] = {
    lazy val query =
      usrGrpTable joinLeft usrInfoTable.sortBy(_.name.nullsLast) on {
        _.feideEmail === _.secId
      }

    db.run(query.result)
      .map { res =>
        MusitSuccess(
          res
            .foldLeft(Seq.empty[RegisteredUser]) { (users, curr) =>
              val feideEmail = curr._1.feideEmail
              val userInfo = curr._2.map { u =>
                UserInfo(
                  id = u._1,
                  secondaryIds = u._2.map(fe => Seq(fe.value)),
                  name = u._3,
                  email = u._4,
                  picture = u._5
                )
              }

              users.zipWithIndex
                .find(_._1.feideEmail == feideEmail)
                .map { found =>
                  val uinf = found._1.maybeUserInfo.orElse(userInfo)
                  users.updated(found._2, found._1.copy(maybeUserInfo = uinf))
                }
                .getOrElse {
                  val ru = RegisteredUser(curr._1.feideEmail, userInfo)
                  ru +: users
                }
            }
            .sortBy(_.maybeUserInfo.isDefined)
            .reverse
        )
      }
      .recover {
        case NonFatal(ex) =>
          handleError(s"An error occurred trying to fetch registered users.", ex)
      }
  }
}
