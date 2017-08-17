package models.elasticsearch

case class IndexCallback(success: IndexName => Unit, failure: () => Unit)
