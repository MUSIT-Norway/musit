import no.uio.musit.microservice.event.domain.BaseEventDto
import org.scalatestplus.play.PlaySpec

import scala.util.Try

class BaseEventDTOTest extends PlaySpec {
  def createDtoWithValueLong(value: Option[Long]) = {

    val date = new java.util.Date()
    val timestamp = new java.sql.Timestamp(date.getTime)

    BaseEventDto(None, None, null, None, Seq.empty, None, Seq.empty, None, valueLong = value, None, None, Some("nobody"), Some(timestamp)
    )
  }

  "getOptBool" must {
    "work when valueLong is 0" in {
      val be = createDtoWithValueLong(Some(0))
      val t = Try(be.getOptBool)
      t.get mustBe Some(false)
    }
    "work when valueLong is 1" in {
      val be = createDtoWithValueLong(Some(1))
      val t = Try(be.getOptBool)
      t.get mustBe Some(true)
    }
    "crash when valueLong is 2" in {
      val be = createDtoWithValueLong(Some(2))
      Try(be.getOptBool).isSuccess mustBe false
    }
    "work when valueLong is None " in {
      val be = createDtoWithValueLong(None)
      be.getOptBool mustBe None
    }
  }

  "setBool" must {
    "set valueLong to 1 if value is true" in {
      val be = createDtoWithValueLong(None)
      val beUpdated = be.setBool(true)
      beUpdated.valueLong mustBe Some(1)
    }
    "set valueLong to 0 if value is false" in {
      val be = createDtoWithValueLong(None)
      val beUpdated = be.setBool(false)
      beUpdated.valueLong mustBe Some(0)
    }
  }
}