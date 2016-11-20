package controllers.web.forms

import no.uio.musit.security.Permissions._
import play.api.data.Form
import play.api.data.Forms._

case class GroupForm(name: String, permission: Int, description: Option[String])

object GroupForm {
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
    )(GroupForm.apply)(GroupForm.unapply)
  )
}