package dao

import com.google.inject.Inject
import models.{ NodeId, ObjectAggregation, ObjectId }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.driver.JdbcProfile
import slick.jdbc.GetResult

import scala.concurrent.Future

class ObjectAggregationDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  def getObjects(nodeId: Long): Future[Seq[ObjectAggregation]] = {
    Future.successful(Seq(
      ObjectAggregation(ObjectId(1), "øks", "C666", Some("1a"), NodeId(1))
    ))
  }

  def createCoffees: DBIO[Int] =
    sqlu"""create table coffees(
    name varchar not null,
    sup_id int not null,
    price double not null,
    sales int not null,
    total int not null,
    foreign key(sup_id) references suppliers(id))"""

  def createSuppliers: DBIO[Int] =
    sqlu"""create table suppliers(
    id int not null primary key,
    name varchar not null,
    street varchar not null,
    city varchar not null,
    state varchar not null,
    zip varchar not null)"""

  def insertSuppliers: DBIO[Unit] = DBIO.seq(
    // Insert some suppliers
    sqlu"insert into suppliers values(101, 'Acme, Inc.', '99 Market Street', 'Groundsville', 'CA', '95199')",
    sqlu"insert into suppliers values(49, 'Superior Coffee', '1 Party Place', 'Mendocino', 'CA', '95460')",
    sqlu"insert into suppliers values(150, 'The High Ground', '100 Coffee Lane', 'Meadows', 'CA', '93966')"
  )

  // Case classes for our data
  case class Supplier(id: Int, name: String, street: String, city: String, state: String, zip: String)
  case class Coffee(name: String, supID: Int, price: Double, sales: Int, total: Int)

  // Result set getters
  implicit val getSupplierResult = GetResult(r => Supplier(r.nextInt, r.nextString, r.nextString,
    r.nextString, r.nextString, r.nextString))
  implicit val getCoffeeResult = GetResult(r => Coffee(r.<<, r.<<, r.<<, r.<<, r.<<))

  val price = 1

  val test = sql"""select c.name, s.name
      from coffees c, suppliers s
      where c.price < $price and s.id = c.sup_id""".as[(String, String)]

  val name = "Starbucks"
  val table = "coffees"
  sql"select * from #$table where name = $name".as[Coffee].headOption

  /* sql: select */
  /* sql-spørring som gir musitThing-objekter til en gitt node
 select object_id,displayName,displayId from MUSIT_MAPPING.VIEW_MUSITTHING VM,MUSARK_STORAGE.LOCAL_OBJECT L
  WHERE L.current_location_id = NODE_ID and l.object_id = vm.ny_id*/
}
