package no.uio.musit.security

import play.api.Logger

/**
 * Created by jarle on 28.09.16.
 */

object SecurityGroups {

  val logger = Logger(classOf[SecurityGroup])

  sealed trait Permission {

  }

  object Read extends Permission {

  }

  object Write extends Permission {

  }

  object Admin extends Permission {

  }

  sealed trait SecurityGroup {
    val groupId: String
    val permissions: Seq[Permission]
  }

  case object KhmSfRead extends SecurityGroup {

    val groupId = "todo1" //TODO:
    val permissions = Seq(Read)
  }

  case object KhmSfWrite extends SecurityGroup {
    val groupId = "todo2" //TODO:
    val permissions = Seq(Read, Write)
  }

  case object KhmSfAdmin extends SecurityGroup {
    val groupId = "todo3" //TODO:
    val permissions = Seq(Read, Write, Admin)
  }

  def fromGroupId(groupId: String): Option[SecurityGroup] = {
    groupId match {
      case KhmSfRead.groupId => Some(KhmSfRead)
      case KhmSfWrite.groupId => Some(KhmSfWrite)
      case KhmSfAdmin.groupId => Some(KhmSfAdmin)
      case unknownGroupId =>
        logger.info(s"Unknown groupId: $unknownGroupId")
        None
    }
  }
}
