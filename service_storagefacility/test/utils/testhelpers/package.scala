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

  val BaseUrl = "/v1/museum"
  val StorageNodesUrl = (mid: Int) => s"$BaseUrl/$mid/storagenodes"
  val RootNodeUrl = (mid: Int) => s"$BaseUrl/$mid/storagenodes/root"
  val StorageNodeUrl = (mid: Int, node: Long) => s"$BaseUrl/$mid/storagenodes/$node"
  val MoveStorageNodeUrl = (mid: Int) => s"${StorageNodesUrl(mid)}/moveNode"
  val MoveObjectUrl = (mid: Int) => s"${StorageNodesUrl(mid)}/moveObject"
  val NodeChildrenUrl = (mid: Int, node: Long) => s"${StorageNodeUrl(mid, node)}/children"
  val ObjLocationHistoryUrl = (mid: Int, objectId: Long) => s"${StorageNodesUrl(mid)}/objects/$objectId/locations" // scalastyle:ignore

  val ControlsUrl = (mid: Int, node: Long) => s"${StorageNodeUrl(mid, node)}/controls"
  val ControlUrl = (mid: Int, node: Long, evt: Long) => s"${ControlsUrl(mid, node)}/$evt"
  val ObservationsUrl = (mid: Int, node: Long) => s"${StorageNodeUrl(mid, node)}/observations" // scalastyle:ignore
  val ObservationUrl = (mid: Int, node: Long, evt: Long) => s"${ObservationsUrl(mid, node)}/$evt" // scalastyle:ignore
  val CtrlObsForNodeUrl = (mid: Int, node: Long) => s"${StorageNodeUrl(mid, node)}/events"
  val KdReportUrl = (mid: Int) => s"$BaseUrl/$mid/storagenodes/report"
  val ObjCurrentLocationUrl = (mid: Int, objectId: Long) => s"${StorageNodesUrl(mid)}/objects/$objectId/currentlocation" // scalastyle:ignore
  val StorageNodeSearchName = (mid: Int) => s"${StorageNodesUrl(mid)}/search"

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
