package models

/**
 * Created by jarle on 10.10.16.
 */

import models.dto.MusitThingDto
import org.scalatestplus.play.PlaySpec

class MuseumNoSpec extends PlaySpec {
  "Interacting with MuseumNo" when {
    def numberPart(museumNo: String): Option[Long] = {
      MusitThingDto.museumNoNumberPart(museumNo)
    }

    numberPart("C4252") mustBe Some(4252)
    numberPart("4252") mustBe Some(4252)
    numberPart("C") mustBe None
    numberPart("") mustBe None
    numberPart("C4252a") mustBe Some(4252)
    numberPart("C.4252a") mustBe Some(4252)
    numberPart("C. 4252a") mustBe Some(4252)
    numberPart("B3241/a_7") mustBe Some(3241)
    numberPart("B4610/II_æ") mustBe Some(4610)
  }

  "Interacting with SubNo" when {
    def numberPart(subNo: String): Option[Long] = {
      MusitThingDto.subNoNumberPart(subNo)
    }

    numberPart("10a") mustBe Some(10)
    numberPart("1a") mustBe Some(1)
    numberPart("123") mustBe Some(123)
    numberPart("4252A") mustBe Some(4252)
    numberPart("U") mustBe None
    numberPart("") mustBe None
    numberPart("U123") mustBe Some(123)
    numberPart("U123.") mustBe Some(123)
    numberPart("UT123.") mustBe Some(123)
  }
}
