package no.uio.musit.microservice.storageAdmin.dao

import no.uio.musit.microservice.storageAdmin.domain.StorageAdmin
import no.uio.musit.microservices.common.linking.LinkService
import play.api.Play
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}
import slick.driver.JdbcProfile

import scala.concurrent.Future

/**
  * Created by ellenjo on 5/18/16.
  */
object StorageAdminDao extends HasDatabaseConfig[JdbcProfile] {
  import driver.api._
  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val StorageAdminTable = TableQuery[StorageAdminTable]


  def getWholeCollectionStorage(storageCollectionRoot:String) : Future[Seq[StorageAdmin]] = {
    val action = StorageAdminTable.filter( _.storageType === storageCollectionRoot).result
    db.run(action)
  }

  def all() : Future[Seq[StorageAdmin]] = db.run(StorageAdminTable.result)

  def insert(storageAdmin: StorageAdmin): Future[StorageAdmin] = {
  val insertQuery = StorageAdminTable returning StorageAdminTable.map(_.id) into ((storageAdmin, id) => storageAdmin.copy(id=id,links=Seq(LinkService.self(s"/v1/$id"))))
  val action = insertQuery += storageAdmin
  db.run(action)
}

  /* def getDisplayID(id:Long) :Future[Option[String]] ={
  val action = MusitThingTable.filter( _.id === id).map(_.displayid).result.headOption
  db.run(action)
}*/
  def updateStorageAdminByID(id:Long,storageAdmin:StorageAdmin) = {
    StorageAdminTable.filter(_.id === id).update(storageAdmin)
  }

  def updateStorageNameByID(id:Long,storageName:String) = {
  val u = for { l <- StorageAdminTable if l.id === id
  } yield l.storageName
    u.update(storageName)
  }

  def getById(id:Long) :Future[Option[StorageAdmin]] ={
  val action = StorageAdminTable.filter( _.id === id).result.headOption
  db.run(action)
}

  private class StorageAdminTable(tag: Tag) extends Table[StorageAdmin](tag, Some("MUSARK_STORAGE"),"STORAGE_UNIT") {
        def * = (id,storageName,storageType) <>(create.tupled, destroy)

def id = column[Long]("STORAGE_UNIT_ID", O.PrimaryKey, O.AutoInc)

    def storageName = column[String]("STORAGE_UNIT_NAME")

    def storageType = column[String]("STORAGE_TYPE")

    def create = (id: Long,storageName:String, storageType:String ) => StorageAdmin(id, storageName, storageType,Seq(LinkService.self(s"/v1/$id")))

    def destroy(admin:StorageAdmin) = Some(admin.id, admin.storageUnitName, admin.storageType)
  }
}
