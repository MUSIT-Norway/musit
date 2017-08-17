package models.elasticsearch

case class IndexName(underlying: String) {
  override def toString: String = underlying

  def name: String = underlying
}
