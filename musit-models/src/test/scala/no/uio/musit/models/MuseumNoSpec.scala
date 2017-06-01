package no.uio.musit.models

import org.scalatest.{MustMatchers, WordSpec}

class MuseumNoSpec extends WordSpec with MustMatchers {

  "Interacting with MuseumNo" should {
    "extract number part when prefixed with a character" in {
      MuseumNo("C4252").asNumber mustBe Some(4252)
    }

    "extract number part when not prefixed or suffixed with characters" in {
      MuseumNo("4252").asNumber mustBe Some(4252)
    }

    "not extract a number if only containing a character" in {
      MuseumNo("C").asNumber mustBe None
    }

    "not extract a number if empty" in {
      MuseumNo("").asNumber mustBe None
    }

    "extract a number if prefix and suffixed with characters" in {
      MuseumNo("C4252a").asNumber mustBe Some(4252)
    }

    "extract a number if prefixed with a character and a ., and has suffix" in {
      MuseumNo("C.4252a").asNumber mustBe Some(4252)
    }

    "extract a number if prefix with character, . and whitespace, and has suffix" in {
      MuseumNo("C. 4252a").asNumber mustBe Some(4252)
    }

    "extract number if prefixed with character and has suffix starting with /" in {
      MuseumNo("B3241/a_7").asNumber mustBe Some(3241)
      MuseumNo("B4610/II_æ").asNumber mustBe Some(4610)
    }

  }
}
