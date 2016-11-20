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

import no.uio.musit.models.GroupId
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
    roleId: GroupId,
    museum: Museum,
    permissions: Seq[Permission]
  )

  val AppAdmin = GroupId.unsafeFromString("c0f20097-e803-4d1f-9d86-7b36bcfaec19")

  val TestSfRead = GroupId.unsafeFromString("2d503a2e-2211-45dd-a99f-fe1a38b5f2a2")
  val TestSfWrite = GroupId.unsafeFromString("c81c314c-0675-4cd1-8956-c96a7163825b")
  val TestSfAdmin = GroupId.unsafeFromString("bc4b4d44-9470-4622-8e29-03f0bfaf5149")

  val KhmSfRead = GroupId.unsafeFromString("3ce7692d-2101-45a4-955b-0ca861540cd9")
  val KhmSfWrite = GroupId.unsafeFromString("fd34b019-81e1-47a2-987b-64389d6fce04")
  val KhmSfAdmin = GroupId.unsafeFromString("de48b2dd-f25c-4b06-a5d4-7de45780ef2e")

  val NhmSfRead = GroupId.unsafeFromString("92e3b487-c962-43d9-a2ea-b8a7bed1b67a")
  val NhmSfWrite = GroupId.unsafeFromString("f311dd04-89b8-48a4-b4cd-68092f76ebab")
  val NhmSfAdmin = GroupId.unsafeFromString("5aa16994-92b3-45df-adfa-7f9fdf54ce34")

  val UmSfRead = GroupId.unsafeFromString("b923b5df-54d3-4724-9386-c1273561f1a1")
  val UmSfWrite = GroupId.unsafeFromString("6ae158a1-398c-4f05-9992-1e19759e3221")
  val UmSfAdmin = GroupId.unsafeFromString("0ccefaff-aee3-4447-98e5-ec29bb9b80b7")

  val AmSfRead = GroupId.unsafeFromString("428f6108-f376-47e5-80c2-0e166e75ae42")
  val AmSfWrite = GroupId.unsafeFromString("f6a43a29-c0ee-419f-b315-d343629fb9b8")
  val AmSfAdmin = GroupId.unsafeFromString("3a3a173e-cf99-4301-9988-78fcd5d3d153")

  val VmSfRead = GroupId.unsafeFromString("3eb0b341-6c16-46f7-bd75-b0e12ba6cb8b")
  val VmSfWrite = GroupId.unsafeFromString("b4beaefb-d124-4f10-874d-ef59ffa1bb3b")
  val VmSfAdmin = GroupId.unsafeFromString("cd9ea5bf-7972-45c2-a0fd-b7e75fc2e5db")

  val TmuSfRead = GroupId.unsafeFromString("b08efccf-a4a7-41b0-b208-acc8fc9c9bb4")
  val TmuSfWrite = GroupId.unsafeFromString("4f8204c3-d48f-4a7c-9c79-1136edc337fb")
  val TmuSfAdmin = GroupId.unsafeFromString("611202c5-8c69-444d-9b82-6aac5be150e4")

  val KmnSfRead = GroupId.unsafeFromString("b1bd705b-a6b4-48ee-b517-f1c7be1bb015")
  val KmnSfWrite = GroupId.unsafeFromString("c875ae85-7a01-4594-8e3e-7e4fbc792489")
  val KmnSfAdmin = GroupId.unsafeFromString("53a87b63-4628-482a-b28c-40689129b962")

  val roles: Map[GroupId, Role] = Map(
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

  def fromGroupId(gid: GroupId): Option[Role] = roles.get(gid)

}