package controllers.web.dto

import no.uio.musit.security.Permissions._
import play.api.data.Form
import play.api.data.Forms._

case class GroupDTO(name: String, permission: Int, description: Option[String])

object GroupDTO {
  val allowedGroups = scala.collection.immutable.Seq(
    (GodMode.priority.toString, GodMode.productPrefix),
    (Admin.priority.toString, Admin.productPrefix),
    (Write.priority.toString, Write.productPrefix),
    (Read.priority.toString, Read.productPrefix),
    (Guest.priority.toString, Guest.productPrefix)
  )

  val groupForm = Form(
    mapping(
      "name" -> text(minLength = 3),
      "permission" -> number.verifying(n => allowedGroups.exists(_._1 == n.toString)),
      "description" -> optional(text)
    )(GroupDTO.apply)(GroupDTO.unapply)
  )
}