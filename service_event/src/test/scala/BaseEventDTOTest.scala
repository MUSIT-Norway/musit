import no.uio.musit.microservice.event.dto.BaseEventDto
import org.scalatestplus.play.PlaySpec

import scala.util.Try

class BaseEventDTOTest extends PlaySpec {
  "getOptBool" must {
    "work when valueLong is 0" in {
      val be = BaseEventDto(None, None, null, None, Seq(), None, valueLong = Some(0), None, None)
      val t = Try(be.getOptBool)
      t.get mustBe Some(false)
    }
    "work when valueLong is 1" in {
      val be = BaseEventDto(None, None, null, None, Seq(), None, valueLong = Some(1), None, None)
      val t = Try(be.getOptBool)
      t.get mustBe Some(true)
    }
    "crash when valueLong is 2" in {
      val be = BaseEventDto(None, None, null, None, Seq(), None, valueLong = Some(2), None, None)
      Try(be.getOptBool).isSuccess mustBe false
    }
    "work when valueLong is None " in {
      val be = BaseEventDto(None, None, null, None, Seq(), None, valueLong = None, None, None)
      be.getOptBool mustBe None
    }
  }

  "setBool" must {
    "set valueLong to 1 if value is true" in {
      val be = BaseEventDto(None, None, null, None, Seq(), None, valueLong = None, None, None)
      val beUpdated = be.setBool(true)
      beUpdated.valueLong mustBe Some(1)
    }
    "set valueLong to 0 if value is false" in {
      val be = BaseEventDto(None, None, null, None, Seq(), None, valueLong = None, None, None)
      val beUpdated = be.setBool(false)
      beUpdated.valueLong mustBe Some(0)
    }
  }
}