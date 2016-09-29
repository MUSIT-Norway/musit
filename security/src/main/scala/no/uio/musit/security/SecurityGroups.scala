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
    val museums: Seq[Museum]

    def permissionsRelativeToMuseum(museum: Museum) = {
      if (museums.contains(museum)) permissions else Seq.empty
    }
  }

  case object KhmSfRead extends SecurityGroup {

    val groupId = "KhmSfRead" //TODO:
    val permissions = Seq(Read)
    val museums = Seq(Khm)
  }

  case object KhmSfWrite extends SecurityGroup {
    val groupId = "KhmSfWrite" //TODO:
    val permissions = Seq(Read, Write)
    val museums = Seq(Khm)
  }

  case object KhmSfAdmin extends SecurityGroup {
    val groupId = "KhmSfAdmin" //TODO:
    val permissions = Seq(Read, Write, Admin)
    val museums = Seq(Khm)
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
