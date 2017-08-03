package no.uio.musit.models

import org.scalatest.{MustMatchers, WordSpec}

class MusitUUIDSpec extends WordSpec with MustMatchers {

  case class TestUUID(underlying: java.util.UUID) extends MusitUUID

  object TestUUID extends MusitUUIDOps[TestUUID] {
    override implicit def fromUUID(uuid: java.util.UUID): TestUUID = TestUUID(uuid)

    override def generate() = TestUUID(java.util.UUID.randomUUID())
  }

  "MusitUUID" should {
    "fail to initialise from String when too long" in {
      val id = java.util.UUID.randomUUID().toString + "a"

      TestUUID.fromString(id) mustBe None
    }

    "correctly initialise from a valid UUID" in {
      val id = java.util.UUID.randomUUID()

      TestUUID.fromString(id.toString) mustBe Some(TestUUID(id))
    }
  }

}
