package models.analysis

import no.uio.musit.models.ActorId
import org.joda.time.DateTime
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.Json

class ActorStampSpec extends WordSpec with MustMatchers {

  "ActorStamp" should {
    "formatted date in json as iso format" in {
      val dateAsStr = "2017-06-07T08:40:15+00:00"
      val res = Json.toJson(
        ActorStamp(ActorId.generate(), DateTime.parse(dateAsStr))
      )

      (res \ "date").as[String] mustBe dateAsStr
    }
  }

}
