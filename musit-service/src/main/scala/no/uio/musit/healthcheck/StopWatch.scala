package no.uio.musit.healthcheck

case class StopWatch(
    ticker: Ticker = SystemTicker()
) {
  val startTick = ticker.tick()

  def elapsed(): Long =
    ticker.tick() - startTick

}

trait Ticker {
  def tick(): Long
}

class SystemTicker extends Ticker {
  override def tick() = System.currentTimeMillis()
}

object SystemTicker {
  def apply(): SystemTicker = new SystemTicker()
}
