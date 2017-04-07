package no.uio.musit.models

import org.scalatest.{Inside, MustMatchers, WordSpec}

class ExternalRefSpec extends WordSpec with MustMatchers with Inside {

  "ExternalRef" should {
    "have pipe separator around one ref" in {
      ExternalRef(Seq("a")).toDbString mustBe "|a|"
    }

    "have pipe separator between multiple ref" in {
      ExternalRef(Seq("a", "b")).toDbString mustBe "|a|b|"
    }

    "parse multiple ref" in {
      ExternalRef("|a|b|").underlying must contain allOf ("a", "b")
    }
  }

}
