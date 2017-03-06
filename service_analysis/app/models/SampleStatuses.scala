package models

object SampleStatuses {

  sealed trait SampleStatus {
    val identity: Int
  }

  case object Ok extends SampleStatus {
    override val identity = 1
  }

  case object Destroyed extends SampleStatus {
    override val identity = 2
  }

  case object Contaminated extends SampleStatus {
    override val identity = 3
  }

  case object Prepared extends SampleStatus {
    override val identity = 4
  }

}
