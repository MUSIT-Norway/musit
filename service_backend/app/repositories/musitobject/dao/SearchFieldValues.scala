package repositories.musitobject.dao

object SearchFieldValues {

  sealed trait IntervalBoundary {}
  case class Value(v: Long) extends IntervalBoundary
  case class Infinite()     extends IntervalBoundary

  def boundaryAsString(intervalBoundary: IntervalBoundary) = {
    intervalBoundary match {
      case Value(v)   => v.toString()
      case Infinite() => ""
    }
  }

  //case class IntervalValue(from: IntervalBoundary, to: IntervalBoundary)

  sealed trait FieldValue {
    val v: String
  }

  case class EmptyValue() extends FieldValue {
    override val v: String = ""
  }

  case class LiteralValue(v: String) extends FieldValue

  /**
   * If v contains a value which needs to be escaped, escapeChar contains the
   * appropriate escape character. If v doesn't contains a value which needs to be
   * escaped with the given escapeChar.
   */
  case class WildcardValue(v: String, escapeChar: Char) extends FieldValue

  case class IntervalValue(from: IntervalBoundary, to: IntervalBoundary)
      extends FieldValue {
    override val v: String = boundaryAsString(from) + ".." + boundaryAsString(to)

  }

}
