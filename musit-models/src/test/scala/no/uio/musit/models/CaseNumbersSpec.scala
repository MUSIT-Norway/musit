package no.uio.musit.models

import org.scalatest.{Inside, MustMatchers, WordSpec}

class CaseNumbersSpec extends WordSpec with MustMatchers with Inside {

  "CaseNumber" should {
    "have pipe separator around one id" in {
      CaseNumbers(Seq("a")).toDbString mustBe "|a|"
    }

    "have pipe separator between multiple ids" in {
      CaseNumbers(Seq("a", "b")).toDbString mustBe "|a|b|"
    }

    "parse multiple ids" in {
      CaseNumbers("|a|b|").underlying must contain allOf ("a", "b")
    }
  }

}
