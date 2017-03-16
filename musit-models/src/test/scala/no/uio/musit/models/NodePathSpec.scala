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

package no.uio.musit.models

import org.scalatest.{MustMatchers, WordSpec}

class NodePathSpec extends WordSpec with MustMatchers {

  "NodePath" should {

    "create a new instance with valid path argument" in {
      NodePath(",1,2,3,4,5,6,7,").path mustBe ",1,2,3,4,5,6,7,"
    }

    "create a new instance if path is missing leading comma" in {
      NodePath("1,2,3,4,5,6,7,").path mustBe ",1,2,3,4,5,6,7,"
    }

    "create a new instance if path is missing trailing comma" in {
      NodePath(",1,2,3,4,5,6,7").path mustBe ",1,2,3,4,5,6,7,"
    }

    "create a new instance if path is missing leading and trailing comma" in {
      NodePath("1,2,3,4,5,6,7").path mustBe ",1,2,3,4,5,6,7,"
    }

    "fail with IllegalArgumentException if path contains non-integer value" in {
      intercept[IllegalArgumentException] {
        NodePath(",1,2,abc,4,5,6,7")
      }
    }

    "fail with IllegalArgumentException if path contains ,," in {
      intercept[IllegalArgumentException] {
        NodePath(",1,2,,4,5,6,7")
      }
    }

    "return the parent path" in {
      NodePath("1,2,3,4,5,6,7").parent.path mustBe ",1,2,3,4,5,6,"
    }

    "append a new path element" in {
      NodePath("1,2,3,4,5,6,7")
        .appendChild(StorageNodeDatabaseId(8))
        .path mustBe ",1,2,3,4,5,6,7,8,"
    }

    "append a new path element to NodePath.empty" in {
      NodePath.empty.appendChild(StorageNodeDatabaseId(3)).path mustBe ",3,"
    }

    "return NodePath.empty when trying get the parent of NodePath.empty" in {
      NodePath.empty.parent mustBe NodePath.empty
    }
  }

}
