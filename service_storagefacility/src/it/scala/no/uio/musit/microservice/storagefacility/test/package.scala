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

package no.uio.musit.microservice.storagefacility

package object test {

  val BaseUrl = "/v1/storagenodes"
  val StorageNodesUrl = BaseUrl
  val RootNodeUrl = s"$BaseUrl/root"
  val StorageNodeUrl = (node: Long) => s"$BaseUrl/$node"
  val MoveStorageNodeUrl = s"$StorageNodesUrl/moveNode"
  val MoveObjectUrl = s"$StorageNodesUrl/moveObject"
  val NodeChildrenUrl = (node: Long) => s"${StorageNodeUrl(node)}/children"

  val ControlsUrl = (node: Long) => s"${StorageNodeUrl(node)}/controls"
  val ControlUrl = (node: Long, evt: Long) => s"${ControlsUrl(node)}/$evt"
  val ObservationsUrl = (node: Long) => s"${StorageNodeUrl(node)}/observations"
  val ObservationUrl = (node: Long, evt: Long) => s"${ObservationsUrl(node)}/$evt"

  val CtrlObsForNodeUrl = (node: Long) => s"${StorageNodeUrl(node)}/events"
  val KdReportUrl = s"$BaseUrl/report"



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
      | """.stripMargin.replace('\n', ' ')

}
