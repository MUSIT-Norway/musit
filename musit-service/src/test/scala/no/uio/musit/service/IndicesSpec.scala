package no.uio.musit.service

import org.scalatest.{MustMatchers, WordSpec}

class IndicesSpec extends WordSpec with MustMatchers {

  "Parsing a query string argument" should {

    "split the an array formatted string into a sorted list of Strings" in {
      val str = "[foo,bar,baz]"
      Indices.getFrom(str) mustBe List("bar", "baz", "foo")
    }

    "trim all whitespaces from the parsed string values" in {
      val str = "[ foo  , bar, baz ]"
      Indices.getFrom(str) mustBe List("bar", "baz", "foo")
    }

  }

}
