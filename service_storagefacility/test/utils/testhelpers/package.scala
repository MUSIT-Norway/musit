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

package utils

package object testhelpers {

  val BaseUrl            = "/museum"
  val StorageNodesUrl    = (mid: Int) => s"$BaseUrl/$mid/storagenodes"
  val RootNodeUrl        = (mid: Int) => s"$BaseUrl/$mid/storagenodes/root"
  val StorageNodeUrl     = (mid: Int, node: String) => s"$BaseUrl/$mid/storagenodes/$node"
  val MoveStorageNodeUrl = (mid: Int) => s"${StorageNodesUrl(mid)}/moveNode"
  val MoveObjectUrl      = (mid: Int) => s"${StorageNodesUrl(mid)}/moveObject"

  val NodeChildrenUrl = (mid: Int, node: String) =>
    s"${StorageNodeUrl(mid, node)}/children"

  val ObjLocationHistoryUrl = (mid: Int, objectId: String) =>
    s"${StorageNodesUrl(mid)}/objects/$objectId/locations"

  val ControlsUrl = (mid: Int, node: String) => s"${StorageNodeUrl(mid, node)}/controls"
  val ControlUrl = (mid: Int, node: String, evt: Long) =>
    s"${ControlsUrl(mid, node)}/$evt"

  val ObservationsUrl = (mid: Int, node: String) =>
    s"${StorageNodeUrl(mid, node)}/observations"

  val ObservationUrl = (mid: Int, node: String, evt: Long) =>
    s"${ObservationsUrl(mid, node)}/$evt"

  val CtrlObsForNodeUrl = (mid: Int, node: String) =>
    s"${StorageNodeUrl(mid, node)}/events"

  val KdReportUrl = (mid: Int) => s"$BaseUrl/$mid/storagenodes/report"

  val ObjCurrentLocationUrl = (mid: Int, objectId: String) =>
    s"${StorageNodesUrl(mid)}/objects/$objectId/currentlocation"

  val StorageNodeSearchName = (mid: Int) => s"${StorageNodesUrl(mid)}/search"

  val ScanUrl = (mid: Int) => s"${StorageNodesUrl(mid)}/scan"

  val HundredAndOneCharString =
    """abcdefghijklmnopqrstuvwxyzæøåa
      |abcdefghijklmnopqrstuvwxyzæøåa
      |abcdefghijklmnopqrstuvwxyzæøåa
      |abcdefghijk""".stripMargin.replaceAll("\n", "")

  val VeryLongString =
    """12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |""".stripMargin.replaceAll("\n", " ")

}
