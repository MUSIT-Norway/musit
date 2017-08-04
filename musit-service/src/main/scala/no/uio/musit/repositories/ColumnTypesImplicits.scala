package no.uio.musit.repositories

import play.api.db.slick.HasDatabaseConfig
import slick.jdbc.JdbcProfile

/**
 * Column type mappers doesn't need to import all the implicits from {{{profile.api._}}}.
 * To reduce the amount of implicit in scope for those traits we can use this trait
 * and replace {{{import profile.api._}}} with explicit
 * {{{import profile.api.{MappedColumnType, BaseColumnType}}} and
 * {{{import columnTypes._}}}
 */
trait ColumnTypesImplicits { self: HasDatabaseConfig[JdbcProfile] =>

  import profile.ImplicitColumnTypes

  object columnTypes extends ImplicitColumnTypes

}
