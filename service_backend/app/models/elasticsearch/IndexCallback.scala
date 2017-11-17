package models.elasticsearch

/**
 * Since the onComplete is always called we need to keep a record so the callback
 * isn't called twice on failures.
 */
case class IndexCallback(
    private val success: IndexConfig => Unit,
    private val failure: Throwable => Unit
) {
  private var callbackIsUsed: Boolean = true

  def onSuccess(cfg: IndexConfig): Unit = {
    if (callbackIsUsed) {
      success(cfg)
      callbackIsUsed = false
    }
  }

  def onFailure(t: Throwable): Unit = {
    if (callbackIsUsed) {
      failure(t)
      callbackIsUsed = false
    }
  }

  def used: Boolean = !callbackIsUsed

  def notUsed: Boolean = callbackIsUsed

}
