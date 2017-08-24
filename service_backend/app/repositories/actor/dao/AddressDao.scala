package repositories.actor.dao

import com.google.inject.{Inject, Singleton}
import models.actor.OrganisationAddress
import no.uio.musit.MusitResults._
import no.uio.musit.models.{DatabaseId, OrgId}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import repositories.shared.dao.ColumnTypeMappers
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddressDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
    with ColumnTypeMappers {

  import profile.api._

  private val orgAdrTable = TableQuery[OrganisationAddressTable]

  def allFor(id: OrgId): Future[Seq[OrganisationAddress]] = {
    db.run(orgAdrTable.filter(_.organisationId === id).result)
  }

  def getById(orgId: OrgId, id: DatabaseId): Future[Option[OrganisationAddress]] = {
    db.run(orgAdrTable.filter { a =>
      a.id === id && a.organisationId === orgId
    }.result.headOption)
  }

  def insert(address: OrganisationAddress): Future[OrganisationAddress] = {
    val insertQuery = orgAdrTable returning
      orgAdrTable.map(_.id) into ((addr, id) => addr.copy(id = id))

    val action = insertQuery += address

    db.run(action)
  }

  def update(orgAddr: OrganisationAddress): Future[MusitResult[Option[Int]]] = {
    val query = orgAdrTable.filter(_.id === orgAddr.id).update(orgAddr)
    db.run(query).map {
      case upd: Int if upd == 0 => MusitSuccess(None)
      case upd: Int if upd == 1 => MusitSuccess(Some(upd))
      case upd: Int if upd > 1  => MusitDbError("Too many records were updated")
    }
  }

  def delete(id: DatabaseId): Future[Int] = {
    db.run(orgAdrTable.filter(_.id === id).delete)
  }

  private class OrganisationAddressTable(
      tag: Tag
  ) extends Table[OrganisationAddress](tag, Some(SchemaName), OrgAdrTableName) {

    val id              = column[Option[DatabaseId]]("ORGADDRESSID", O.PrimaryKey, O.AutoInc)
    val organisationId  = column[Option[OrgId]]("ORG_ID")
    val streetAddress   = column[Option[String]]("STREET_ADDRESS")
    val streetAddress2  = column[Option[String]]("STREET_ADDRESS_2")
    val postalCodePlace = column[String]("POSTAL_CODE_PLACE")
    val countryName     = column[String]("COUNTRY_NAME")

    val create = (
        id: Option[DatabaseId],
        organisationId: Option[OrgId],
        streetAddress: Option[String],
        streetAddress2: Option[String],
        postalCodePlace: String,
        countryName: String
    ) =>
      OrganisationAddress(
        id = id,
        organisationId = organisationId,
        streetAddress = streetAddress,
        streetAddress2 = streetAddress2,
        postalCodePlace = postalCodePlace,
        countryName = countryName
    )

    val destroy = (addr: OrganisationAddress) =>
      Some(
        (
          addr.id,
          addr.organisationId,
          addr.streetAddress,
          addr.streetAddress2,
          addr.postalCodePlace,
          addr.countryName
        )
    )

    // scalastyle:off method.name
    def * =
      (
        id,
        organisationId,
        streetAddress,
        streetAddress2,
        postalCodePlace,
        countryName
      ) <> (create.tupled, destroy)

    // scalastyle:on method.name
  }

}
