/*
 * MUSIT is a museum database to archive natural and cultural history data.
 * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package no.uio.musit.security

import no.uio.musit.models.Museums._
import no.uio.musit.security.Permissions.{Permission, _}
import play.api.Logger

/**
 * TODO: Not too happy about naming here. It's not intuitive that
 * the a Group and a Museum has a direct relation.
 */
object Roles {

  val logger = Logger(classOf[Role])

  case class Role(
    roleId: String,
    museum: Museum,
    permissions: Seq[Permission]
  )

  val NhmSfRead = "NhmSfRead"
  val NhmSfWrite = "NhmSfWrite"
  val NhmSfAdmin = "NhmSfAdmin"
  val KhmSfRead = "KhmSfRead"
  val KhmSfWrite = "KhmSfWrite"
  val KhmSfAdmin = "KhmSfAdmin"
  val UmSfRead = "UmSfRead"
  val UmSfWrite = "UmSfWrite"
  val UmSfAdmin = "UmSfAdmin"
  val EtnoRead = "EtnoRead"
  val EtnoWrite = "EtnoWrite"
  val FotoRead = "FotoRead"
  val FotoWrite = "FotoWrite"

  val roles: Map[String, Role] = Map(
    NhmSfRead -> Role(NhmSfRead, Nhm, Seq(Read)),
    NhmSfWrite -> Role(NhmSfWrite, Nhm, Seq(Write)),
    NhmSfAdmin -> Role(NhmSfAdmin, Nhm, Seq(Admin)),
    KhmSfRead -> Role(KhmSfRead, Khm, Seq(Read)),
    KhmSfWrite -> Role(KhmSfWrite, Khm, Seq(Write)),
    KhmSfAdmin -> Role(KhmSfAdmin, Khm, Seq(Admin)),
    UmSfRead -> Role(UmSfRead, Um, Seq(Read)),
    UmSfWrite -> Role(UmSfWrite, Um, Seq(Write)),
    UmSfAdmin -> Role(UmSfAdmin, Um, Seq(Admin)),
    EtnoRead -> Role(EtnoRead, Khm, Seq(Read)),
    EtnoWrite -> Role(EtnoWrite, Khm, Seq(Write)),
    FotoRead -> Role(FotoRead, Khm, Seq(Read)),
    FotoWrite -> Role(FotoWrite, Khm, Seq(Read))
  )

  def fromGroupId(gid: String): Option[Role] = roles.get(gid)

}