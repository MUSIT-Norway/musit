package services.actor

import models.actor.WordList
import no.uio.musit.models.OrgId
import no.uio.musit.service.MusitSearch
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers._

class OrganisationServiceSpec extends MusitSpecWithAppPerSuite with MusitResultValues {

  val service = fromInstanceCache[OrganisationService]

  "The OrganisationService" must {

    "get a list of organisation when tags is empty" in {
      val search = MusitSearch(searchStrings = List("Arkeo"))
      val res    = service.find(search).futureValue
      val id     = OrgId(10)
      res.size must be > 0
      res.headOption.get.id mustBe Some(id)
      res.headOption.get.fn must startWith("Arkeo")
      res.headOption.get.serviceTags mustBe empty
    }

    "get a list of organisation when tags is not empty" in {
      val search = MusitSearch(
        searchMap = Map("tags" -> "storage_facility"),
        searchStrings = List("Kultur")
      )
      val res = service.find(search).futureValue
      val id  = OrgId(1)
      res.size must be > 0
      res.headOption.get.id mustBe Some(id)
      res.headOption.get.fn must startWith("Kultur")
      res.headOption.get.serviceTags mustBe Some(WordList(Seq("storage_facility")))
    }

  }

}
