package no.uio.musit.service

import org.scalatest.{MustMatchers, WordSpec}

class MusitSearchSpec extends WordSpec with MustMatchers {

  "MusitSearch" should {

    "initialize a new instance of MusitSearch when parsing a search String" in {
      val str = "[a=foo, b=bar, c=baz]"

      val res = MusitSearch.parseSearch(str)

      res.searchMap.size mustBe 3
      res.searchMap.keySet must contain allOf ("a", "b", "c")
      res.searchMap.get("a") mustBe Some("foo")
      res.searchMap.get("b") mustBe Some("bar")
      res.searchMap.get("c") mustBe Some("baz")
    }

    "fail if search string doesn't have valid formatting" in {
      val str = "[n=foo, bar, baz=]"

      an[IllegalArgumentException] must be thrownBy MusitSearch.parseSearch(str)
    }

    "return an empty MusitSearch instance" in {
      MusitSearch.parseSearch("") mustBe MusitSearch.empty
    }

    "search with both string and tag" in {
      val search = MusitSearch.parseSearch("[baz, foo=bar]")

      search.searchStrings mustBe List("baz")
      search.searchMap mustBe Map("foo" -> "bar")
    }

  }

}
