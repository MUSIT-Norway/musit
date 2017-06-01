package no.uio.musit.models

case class UserGroupMembership(
    id: Option[Int] = None,
    feideEmail: Email,
    groupId: GroupId,
    collection: Option[CollectionUUID]
)

object UserGroupMembership {

  def applyMulti(
      email: Email,
      grpId: GroupId,
      maybeCollections: Option[Seq[CollectionUUID]]
  ): Seq[UserGroupMembership] = {
    maybeCollections.map { cids =>
      if (cids.nonEmpty) {
        cids.map { cid =>
          UserGroupMembership(
            feideEmail = email,
            groupId = grpId,
            collection = Option(cid)
          )
        }
      } else {
        Seq(
          UserGroupMembership(
            feideEmail = email,
            groupId = grpId,
            collection = None
          )
        )
      }
    }.getOrElse {
      Seq(
        UserGroupMembership(
          feideEmail = email,
          groupId = grpId,
          collection = None
        )
      )
    }
  }

}
