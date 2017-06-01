package no.uio.musit.healthcheck

import no.uio.musit.test.MusitSpec

class StopWatchSpec extends MusitSpec {

  class ListTicker(var values: List[Long]) extends Ticker {
    override def tick() = values match {
      case head :: Nil =>
        head
      case head :: tail =>
        values = tail
        head
      case Nil =>
        throw new IllegalStateException()
    }

  }

  "StopWatch" when {
    "elapsed is called" should {
      "calculate from the first tick" in {
        val ticker = new ListTicker(List(2, 44))
        val sw     = StopWatch(ticker)

        sw.elapsed() mustBe 42
      }
    }
  }

}
