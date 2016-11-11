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

  val AppAdmin = "AppAdmin"

  val TestSfRead = "TestSfRead"
  val TestSfWrite = "TestSfWrite"
  val TestSfAdmin = "TestSfAdmin"

  val AmSfRead = "AmSfRead"
  val AmSfWrite = "AmSfWrite"
  val AmSfAdmin = "AmSfAdmin"

  val UmSfRead = "UmSfRead"
  val UmSfWrite = "UmSfWrite"
  val UmSfAdmin = "UmSfAdmin"

  val KhmSfRead = "KhmSfRead"
  val KhmSfWrite = "KhmSfWrite"
  val KhmSfAdmin = "KhmSfAdmin"

  val NhmSfRead = "NhmSfRead"
  val NhmSfWrite = "NhmSfWrite"
  val NhmSfAdmin = "NhmSfAdmin"

  val VmSfRead = "VmSfRead"
  val VmSfWrite = "VmSfWrite"
  val VmSfAdmin = "VmSfAdmin"

  val TmuSfRead = "TmuSfRead"
  val TmuSfWrite = "TmuSfWrite"
  val TmuSfAdmin = "TmuSfAdmin"

  val KmnSfRead = "KmnSfRead"
  val KmnSfWrite = "KmnSfWrite"
  val KmnSfAdmin = "KmnSfAdmin"

  val roles: Map[String, Role] = Map(
    AppAdmin -> Role(AppAdmin, All, Seq(GodMode)),

    TestSfRead -> Role(TestSfRead, Test, Seq(Read)),
    TestSfWrite -> Role(TestSfWrite, Test, Seq(Write)),
    TestSfAdmin -> Role(TestSfAdmin, Test, Seq(Admin)),

    AmSfRead -> Role(AmSfRead, Am, Seq(Read)),
    AmSfWrite -> Role(AmSfWrite, Am, Seq(Write)),
    AmSfAdmin -> Role(AmSfAdmin, Am, Seq(Admin)),

    UmSfRead -> Role(UmSfRead, Um, Seq(Read)),
    UmSfWrite -> Role(UmSfWrite, Um, Seq(Write)),
    UmSfAdmin -> Role(UmSfAdmin, Um, Seq(Admin)),

    KhmSfRead -> Role(KhmSfRead, Khm, Seq(Read)),
    KhmSfWrite -> Role(KhmSfWrite, Khm, Seq(Write)),
    KhmSfAdmin -> Role(KhmSfAdmin, Khm, Seq(Admin)),

    NhmSfRead -> Role(NhmSfRead, Nhm, Seq(Read)),
    NhmSfWrite -> Role(NhmSfWrite, Nhm, Seq(Write)),
    NhmSfAdmin -> Role(NhmSfAdmin, Nhm, Seq(Admin)),

    VmSfRead -> Role(VmSfRead, Vm, Seq(Read)),
    VmSfWrite -> Role(VmSfWrite, Vm, Seq(Write)),
    VmSfAdmin -> Role(VmSfAdmin, Vm, Seq(Admin)),

    TmuSfRead -> Role(TmuSfRead, Tmu, Seq(Read)),
    TmuSfWrite -> Role(TmuSfWrite, Tmu, Seq(Write)),
    TmuSfAdmin -> Role(TmuSfAdmin, Tmu, Seq(Admin)),

    KmnSfRead -> Role(KmnSfRead, Kmn, Seq(Read)),
    KmnSfWrite -> Role(KmnSfWrite, Kmn, Seq(Write)),
    KmnSfAdmin -> Role(KmnSfAdmin, Kmn, Seq(Admin))
  )

  def fromGroupId(gid: String): Option[Role] = roles.get(gid)

}