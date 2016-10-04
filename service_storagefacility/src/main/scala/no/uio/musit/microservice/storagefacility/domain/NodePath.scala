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

package no.uio.musit.microservice.storagefacility.domain

import no.uio.musit.microservice.storagefacility.domain.storage.StorageNodeId
import play.api.Logger
import play.api.libs.json.Reads._
import play.api.libs.json._

/**
 * Trait defining the shape of a NodePath
 */
trait NodePath {
  /**
   * The actual path value
   */
  def path: String

  /**
   * Function that returns the parent path for the current NodePath. If the
   * current path matches NodePath.empty, the function will return the same
   * result back.
   */
  def parent: NodePath = {
    if (NodePath.empty.path == path) NodePath.empty
    else {
      val stripped = path.stripSuffix(",")
      NodePath(stripped.substring(0, stripped.lastIndexOf(",") + 1))
    }
  }

  /**
   * Appends a child element to the NodePath
   *
   * @param childId StorageNodeId of to append to the path.
   */
  def appendChild(childId: StorageNodeId): NodePath = {
    NodePath(s"$path${childId.underlying},")
  }

  /**
   * Converts the path to a collection of StorageNodeIds
   * @return Seq[StorageNodeId]
   */
  def asIdSeq: Seq[StorageNodeId] = {
    path.stripPrefix(",")
      .stripSuffix(",")
      .split(",")
      .map(s => StorageNodeId(s.toLong))
  }
}

object NodePath {

  val logger = Logger(classOf[NodePath])

  /**
   * Actual implementation of the NodePath trait. It's private to hide direct
   * access to the constructor, because we want to perform validation in the
   * apply method.
   *
   * @param path String with the format that represents a path
   */
  private case class NodePathImpl(path: String) extends NodePath

  // scalastyle:off
  /**
   * Creates a new NodePath instance after validating the syntax.
   *
   * Values that will cause an IllegalArgumentException are:
   * <ul>
   * <li>paths that contain non-numeric values between two commas: {{{,1,asd,}}}</li>
   * <li>paths that contain empty values between two commas: {{{",1,,5,"}}}</li>
   * </ul>
   *
   * @param path the String representing the path of ID's.
   * @return a new instance of NodePath
   * @throws IllegalArgumentException if the syntax is invalid.
   */
  def apply(path: String): NodePath = {
    val p1 = if (!path.startsWith(",")) s",$path" else path
    val p2 = if (!p1.endsWith(",")) s"$p1," else p1

    if (path.contains(",,"))
      throw new IllegalArgumentException(
        s"Requirement failed: Path $path contained empty path element ',,'"
      )

    if (path != empty.path) {
      // Consciously using regular try catch here!
      try {
        p2.trim
          .stripPrefix(",")
          .stripSuffix(",")
          .split(",")
          .foreach(s => s.toLong)
      } catch {
        case nfex: NumberFormatException =>
          logger.error(s"Path $path contained illegal value.")
          throw new IllegalArgumentException(
            s"Requirement failed: Path $path contained non-numeric element."
          )
      }
    }

    NodePathImpl(p2)
  }

  /**
   * Useful function to initialize an empty NodePath.
   */
  val empty: NodePath = NodePathImpl(",")

  // JSON picklers for the NodePath type.
  implicit val reads: Reads[NodePath] = __.read[String].map(NodePath.apply)

  implicit val writes: Writes[NodePath] = Writes { nodePath =>
    JsString(nodePath.path)
  }
}