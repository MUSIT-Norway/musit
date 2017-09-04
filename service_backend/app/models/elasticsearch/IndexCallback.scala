package models.elasticsearch

case class IndexCallback(
    success: IndexConfig => Unit,
    failure: Throwable => Unit
)
