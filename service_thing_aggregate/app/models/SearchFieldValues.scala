package models

object SearchFieldValues {

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

}
