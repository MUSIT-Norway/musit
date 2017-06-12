package repositories.musitobject.dao

import java.util.UUID

import models.musitobject.{ArkCoordinate, ArkLocation, EtnoLocation}
import no.uio.musit.models._
import no.uio.musit.security._
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import org.scalatest.Inspectors.forAll
import utils.testdata.NodeTestData

/**
 * NOTE: Test data for these tests are loaded in the evolution scripts in the
 * src/test/resources directory.
 */
class ObjectDaoSpec
    extends MusitSpecWithAppPerSuite
    with MusitResultValues
    with NodeTestData {

  val dao: ObjectDao = fromInstanceCache[ObjectDao]

  val mid = MuseumId(99)

  val allCollections = Seq(
    MuseumCollection(
      uuid = CollectionUUID(UUID.fromString("2e4f2455-1b3b-4a04-80a1-ba92715ff613")),
      name = Some("Arkeologi"),
      oldSchemaNames = Seq(MuseumCollections.Archeology)
    )
  )

  val lichenCollections = Seq(
    MuseumCollection(
      uuid = CollectionUUID(UUID.fromString("fcb4c598-8b05-4095-ac00-ce66247be38a")),
      name = Some("Lichen"),
      oldSchemaNames = Seq(MuseumCollections.Lichen)
    )
  )

  val dummyUid = ActorId.generate()

  implicit val dummyUser = AuthenticatedUser(
    session = UserSession(
      uuid = SessionUUID.generate(),
      oauthToken = Option(BearerToken(UUID.randomUUID().toString)),
      userId = Option(dummyUid),
      isLoggedIn = true
    ),
    userInfo = UserInfo(
      id = dummyUid,
      secondaryIds = Some(Seq("vader@starwars.com")),
      name = Some("Darth Vader"),
      email = None,
      picture = None
    ),
    groups = Seq(
      GroupInfo(
        id = GroupId.generate(),
        name = "FooBarGroup",
        module = StorageFacility,
        permission = Permissions.Admin,
        museumId = mid,
        description = None,
        collections = allCollections
      )
    )
  )

  val escapeChar = dao.escapeChar

  "The ObjectDao" when {

    "classifying search criteria" should {

      def wildcard(arg: String, expected: String) = {
        val res = dao.classifyValue(Some(arg))
        res.value.v mustBe expected
      }

      "replace '%' with the escape character" in {
        wildcard("C*_A", s"C%${escapeChar}_A")
      }

      "replace '*' with '%' and '%' with the escape character" in {
        wildcard("C*%A", s"C%$escapeChar%A")
      }

      "replace '*' with '%' and prefix '_' with the escape character" in {
        wildcard("*_", s"%${escapeChar}_")
      }

      "replace '*' with '%'" in {
        wildcard("C*A", "C%A")
      }

      "not prefix a single'%' with the escape character" in {
        wildcard("%", "%")
      }

      "not prefix a single '_' with the escape character" in {
        wildcard("_", "_")
      }

    }

    "searching for objects" should {

      "find an existing objects searching with museumNo" in {
        val res = dao
          .search(
            mid,
            1,
            10,
            Some(MuseumNo("C1")),
            None,
            None,
            allCollections
          )
          .futureValue
        res.successValue.matches.length mustBe 10

        val res2 = dao
          .search(
            mid,
            1,
            10,
            Some(MuseumNo("C2")),
            None,
            None,
            allCollections
          )
          .futureValue
          .successValue
        res2.matches.length mustBe 1
      }

      "handle paging correctly" in {
        val res1 = dao
          .search(
            mid,
            1,
            3,
            Some(MuseumNo("C1")),
            None,
            None,
            allCollections
          )
          .futureValue
          .successValue
        res1.matches.length mustBe 3
        res1.matches.head.subNo mustBe Some(SubNo("10a"))
        res1.matches.tail.head.subNo mustBe Some(SubNo("11"))
        res1.matches.last.subNo mustBe Some(SubNo("12"))

        val res2 = dao
          .search(
            mid,
            2,
            3,
            Some(MuseumNo("C1")),
            None,
            None,
            allCollections
          )
          .futureValue
          .successValue
        res2.matches.length mustBe 3
        res2.matches.head.subNo mustBe Some(SubNo("13"))
        res2.matches.tail.head.subNo mustBe Some(SubNo("14"))
        res2.matches.last.subNo mustBe Some(SubNo("15"))

        val res3 = dao
          .search(
            mid,
            3,
            3,
            Some(MuseumNo("C1")),
            None,
            None,
            allCollections
          )
          .futureValue
          .successValue

        res3.matches.length mustBe 3
        res3.matches.head.subNo mustBe Some(SubNo("16"))
        res3.matches.tail.head.subNo mustBe Some(SubNo("17"))
        res3.matches.last.subNo mustBe Some(SubNo("1a"))

        res1.matches must not contain res2.matches
        res1.matches must not contain res3.matches
        res2.matches must not contain res3.matches
      }

      "allow search where museumNo has only digits" in {
        val res = dao
          .search(
            mid,
            1,
            10,
            Some(MuseumNo("777")),
            None,
            None,
            allCollections
          )
          .futureValue
          .successValue

        res.matches.length mustBe 4
        res.matches.head.subNo mustBe Some(SubNo("34"))
        res.matches(1).subNo mustBe Some(SubNo("34A"))
        res.matches(2).subNo mustBe Some(SubNo("34B"))
        res.matches(3).subNo mustBe Some(SubNo("35"))
      }

      "allow wildcard search on museumNo" in {
        val res = dao
          .search(
            mid,
            1,
            10,
            Some(MuseumNo("C555*")),
            None,
            None,
            allCollections
          )
          .futureValue
          .successValue

        res.matches.length mustBe 6
        res.matches.head.subNo mustBe Some(SubNo("34A"))
        res.matches(1).subNo mustBe Some(SubNo("34B"))
        res.matches(2).subNo mustBe Some(SubNo("34C"))
        res.matches(3).museumNo mustBe MuseumNo("C555A")
        res.matches(4).museumNo mustBe MuseumNo("C555B")
        res.matches(5).museumNo mustBe MuseumNo("C555C")
      }

      "return 0 results when attempting SQL-injection" in {
        val res = dao
          .search(
            mid,
            1,
            10,
            Some(MuseumNo("C.' or 1=1 --")),
            None,
            None,
            allCollections
          )
          .futureValue
          .successValue
        res.matches.length mustBe 0
      }

      "find objects using museumNo, subNo with wildcard and term" in {

        val res = dao
          .search(
            mid,
            1,
            10,
            Some(MuseumNo("c555*")),
            Some(SubNo("3*")),
            Some("øks"),
            allCollections // scalastyle:ignore
          )
          .futureValue
          .successValue

        res.matches.length mustBe 3
        res.matches.head.subNo mustBe Some(SubNo("34A"))
        res.matches(1).subNo mustBe Some(SubNo("34B"))
        res.matches(2).subNo mustBe Some(SubNo("34C"))
      }

      "find objects using museumNo with wildcard" in {
        val res = dao
          .search(
            mid,
            1,
            10,
            Some(MuseumNo("c888_*")),
            None,
            Some("øks"),
            allCollections
          )
          .futureValue
          .successValue
        res.matches.length mustBe 2
        res.matches.head.museumNo mustBe MuseumNo("C888_A")
        res.matches.last.museumNo mustBe MuseumNo("C888_B")
      }

      "treat '%' like an ordinary character in equality comparison" in {
        val res = dao
          .search(
            mid,
            1,
            10,
            Some(MuseumNo("C81%A")),
            None,
            None,
            allCollections
          )
          .futureValue
          .successValue
        res.matches.length mustBe 1 //We should find C81%A and *not* C81%XA
        res.matches.head.museumNo mustBe MuseumNo("C81%A")
      }

      "treat '%' like an ordinary character in like comparison" in {
        val res = dao
          .search(
            mid,
            1,
            10,
            Some(MuseumNo("C*%A")),
            None,
            None,
            allCollections
          )
          .futureValue
          .successValue
        res.matches.length mustBe 1
        res.matches.head.museumNo mustBe MuseumNo("C81%A")
      }

      "treat '-' like an ordinary character in equality comparison" in {
        val res = dao
          .search(
            mid,
            1,
            10,
            Some(MuseumNo("C81-A")),
            None,
            None,
            allCollections
          )
          .futureValue
          .successValue
        res.matches.length mustBe 1
        res.matches.head.museumNo mustBe MuseumNo("C81-A")
      }

      "treat '-' like an ordinary character in like comparison" in {
        val res = dao
          .search(
            mid,
            1,
            10,
            Some(MuseumNo("C*-A")),
            None,
            None,
            allCollections
          )
          .futureValue
          .successValue
        res.matches.length mustBe 1
        res.matches.head.museumNo mustBe MuseumNo("C81-A")
      }

      "treat the escape character like an ordinary character equality comparison" in {
        val res = dao
          .search(
            mid,
            1,
            10,
            Some(MuseumNo(s"C81${escapeChar}A")),
            None,
            None,
            allCollections
          )
          .futureValue
          .successValue
        res.matches.length mustBe 1
        res.matches.head.museumNo mustBe MuseumNo(s"C81${escapeChar}A")
      }

      "treat the escape character like an ordinary character like comparison" in {
        val res = dao
          .search(
            mid,
            1,
            10,
            Some(MuseumNo(s"C*${escapeChar}A")),
            None,
            None,
            allCollections
          )
          .futureValue
          .successValue
        res.matches.length mustBe 1
        res.matches.head.museumNo mustBe MuseumNo(s"C81${escapeChar}A")
      }

    }

    "getting objects for a nodeId" should {
      "return a list of objects if the nodeId exists in the museum" in {
        val mr = dao
          .pagedObjects(
            mid = mid,
            nodeId = nodeId4,
            collections = allCollections,
            page = 1,
            limit = 10
          )
          .futureValue
          .successValue

        mr.totalMatches mustBe 3

        mr.matches match {
          case Vector(first, second, third) =>
            first.id mustBe ObjectId(2)
            first.museumNo mustBe MuseumNo("C666")
            first.subNo mustBe Some(SubNo("31"))
            first.term mustBe "Sverd"
            first.arkForm mustBe Some("litt oval")
            first.mainObjectId mustBe None

            second.id mustBe ObjectId(1)
            second.museumNo mustBe MuseumNo("C666")
            second.subNo mustBe Some(SubNo("34"))
            second.term mustBe "Øks"
            second.mainObjectId mustBe None

            third.id mustBe ObjectId(3)
            third.museumNo mustBe MuseumNo("C666")
            third.subNo mustBe Some(SubNo("38"))
            third.term mustBe "Sommerfugl"
            third.arkFindingNo mustBe Some("2017-30")
            third.mainObjectId mustBe None
        }
      }

      "return a list of objects that includes the main object ID" in {
        val mr = dao
          .pagedObjects(
            mid = mid,
            nodeId = nodeId7,
            collections = allCollections,
            page = 1,
            limit = 10
          )
          .futureValue
          .successValue

        mr.totalMatches mustBe 3
        forAll(mr.matches) { m =>
          Seq(m.id) must contain oneOf (ObjectId(48), ObjectId(49), ObjectId(50))
          m.museumNo mustBe MuseumNo("K123")
          m.subNo mustBe None
          Seq(m.term) must contain oneOf ("Kjole", "Drakt", "Skjorte")
          m.mainObjectId mustBe Some(12)
        }

      }

      "return a an empty list when nodeId doesn't exist in museum" in {
        val mr = dao
          .pagedObjects(
            mid = mid,
            nodeId = StorageNodeId.generate(),
            collections = allCollections,
            page = 1,
            limit = 10
          )
          .futureValue
          .successValue
        mr.totalMatches mustBe 0
      }

      "return a an empty vector when museum doesn't exist" in {
        val mr = dao
          .pagedObjects(
            mid = MuseumId(55),
            nodeId = nodeId2,
            collections = allCollections,
            page = 1,
            limit = 10
          )
          .futureValue
          .successValue
        mr.totalMatches mustBe 0
      }

      "return only the number of objects per page specified in limit" in {
        val mr = dao
          .pagedObjects(
            mid = mid,
            nodeId = nodeId6,
            collections = allCollections,
            page = 1,
            limit = 10
          )
          .futureValue
          .successValue
        mr.totalMatches mustBe 32
        mr.matches.size mustBe 10
      }
    }

    "finding the location of an object using an old objectId and schema" should {
      "return the object" in {
        val res =
          dao.findByOldId(111L, "USD_ARK_GJENSTAND_O").futureValue.successValue.value
        res.id mustBe ObjectId(12L)
        res.museumId mustBe mid
        res.term mustBe "Fin øks"
      }

      "return None if not found" in {
        val res = dao.findByOldId(333L, "USD_ARK_GJENSTAND_O").futureValue.successValue
        res mustBe None
      }

    }

    "searching for an object" should {
      "successfully return the object matching the UUID" in {
        val uuid = ObjectUUID.unsafeFromString("dcd37cb7-34ae-484e-a2c0-a1b1925e9b68")
        val mid  = MuseumId(99)
        val res =
          dao.findByUUID(mid, uuid, lichenCollections).futureValue.successValue.value
        res.id mustBe ObjectId(51)
        res.term mustBe "Kartlav"
      }

      "return None if the UUID doesn't exist" in {
        val uuid = ObjectUUID.unsafeFromString("00000000-34ae-484e-a2c0-a1b1925e9b68")
        val mid  = MuseumId(99)
        val res =
          dao.findByUUID(mid, uuid, lichenCollections).futureValue.successValue
        res mustBe None
      }

      "return None when passing in the wrong MuseumId for the object" in {
        val uuid = ObjectUUID.unsafeFromString("dcd37cb7-34ae-484e-a2c0-a1b1925e9b68")
        val mid  = MuseumId(3)
        val res =
          dao.findByUUID(mid, uuid, lichenCollections).futureValue.successValue
        res mustBe None
      }
    }

    "return all children for an object with main object ID" in {
      val mid = MuseumId(99)
      val oid = ObjectUUID.unsafeFromString("aa309693-8d2e-41ae-a958-ada7dc4c914b")
      val res =
        dao.findMainObjectChildren(mid, oid, allCollections).futureValue.successValue

      res.size mustBe 3
      forAll(res) { r =>
        r.mainObjectId mustBe Some(12)
      }
    }

    "return the list of materials for an object using it ObjectId " in {
      val oid          = ObjectId(3)
      val mid          = MuseumId(3)
      val collectionId = MuseumCollections.Collection.fromInt(1)

      val res = dao.getObjectMaterial(mid, collectionId, oid).futureValue.successValue

      res.head.material mustBe Some("tre")
      res.last.material mustBe Some("jern")
    }

    "return the list of locations for an object using it ObjectId " in {
      val oid          = ObjectId(3)
      val mid          = MuseumId(3)
      val collectionId = MuseumCollections.Collection.fromInt(1)

      val res = dao.getObjectLocation(mid, collectionId, oid).futureValue.successValue

      res.size mustBe 2

      forAll(res) { mol =>
        mol mustBe an[ArkLocation]
        val al = mol.asInstanceOf[ArkLocation]
        al.farmName.value must endWith("Berg")
      }
    }

    "return the list of locations for an etno object using ObjectId " in {
      val oid          = ObjectId(55)
      val mid          = MuseumId(99)
      val collectionId = MuseumCollections.Collection.fromInt(2)

      val res = dao.getObjectLocation(mid, collectionId, oid).futureValue.successValue

      res.size mustBe 2

      forAll(res) { mol =>
        mol mustBe an[EtnoLocation]
        val place = mol.asInstanceOf[EtnoLocation]
        place.place.value must startWith("Malmø")
        place.country.value mustBe "Sverige"
        place.region1.value mustBe "Skandinavia"
        place.region2.value mustBe "Norden"
        place.area.value mustBe "Nord-Europa"
      }
    }

    "return the list of coordinates for an archaeology object using ObjectId " in {
      val oid          = ObjectId(3)
      val mid          = MuseumId(99)
      val collectionId = MuseumCollections.Collection.fromInt(1)

      val res = dao.getObjectCoordinate(mid, collectionId, oid).futureValue.successValue

      res.size mustBe 2

      forAll(res) { moc =>
        moc mustBe an[ArkCoordinate]
        val coord = moc.asInstanceOf[ArkCoordinate]
        coord.projection.value must endWith("Sone 32")
        coord.precision.value must include("Stedsnavn")
        coord.north.value mustBe ",6934625,"
        coord.east.value mustBe ",434096,"
      }
    }
  }

}
