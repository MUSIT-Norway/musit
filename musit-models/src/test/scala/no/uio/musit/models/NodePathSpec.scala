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
