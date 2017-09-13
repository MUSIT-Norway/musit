package models.document

import models.document.ArchiveIdentifiers._
import models.document.ArchiveTypes.{ArchiveDocument, DocumentDetails}
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.PersistentType.UserStamp
import net.scalytica.symbiotic.api.types.ResourceParties.{Org, Owner}
import net.scalytica.symbiotic.api.types.{File, FileId, Lock, Path}
import no.uio.musit.models.MuseumCollections
import org.joda.time.DateTime
import org.scalatest.{MustMatchers, OptionValues, WordSpec}

class ArchiveDocumentsSpec extends WordSpec with MustMatchers with OptionValues {

  val archUser  = ArchiveUserId.create()
  val timestamp = DateTime.now().minusDays(7)

  val doc = ArchiveDocument(
    id = ArchiveId.generateAsOpt(),
    fid = FileId.createOpt(),
    title = "foo",
    size = Some("123123"),
    fileType = Some("application/pdf"),
    description = Some("fizz buzz"),
    owner = Some(
      Owner(
        ArchiveOwnerId.create(),
        tpe = Org
      )
    ),
    collection = Some(MuseumCollections.Archeology.uuid),
    path = Some(Path.root),
    lock = Some(
      Lock(
        by = archUser,
        date = timestamp.plusDays(2)
      )
    ),
    version = 1,
    published = false,
    documentMedium = Some("digital"),
    createdStamp = Some(
      UserStamp(
        by = archUser,
        date = timestamp
      )
    ),
    author = Some("Darth Vader"),
    documentDetails = DocumentDetails(
      number = 2,
      docType = Some("test"),
      docSubType = Some("unit test")
    ),
    stream = None
  )

  "An ArchiveDocument" should {

    "be implicitly converted to a symbiotic File" in {
      val symFile: File = doc

      symFile.id.map(_.toString) mustBe doc.id.map(_.asString)
      symFile.filename mustBe doc.title
      symFile.fileType mustBe doc.fileType
      symFile.uploadDate mustBe doc.createdStamp.map(_.date)
      symFile.length mustBe doc.size
      symFile.stream mustBe None

      val md = symFile.metadata
      md.owner mustBe doc.owner
      md.accessibleBy.tail.headOption.map(_.id.value) mustBe doc.collection.map(_.value)
      md.fid mustBe doc.fid
      md.uploadedBy mustBe doc.createdStamp.map(_.by)
      md.version mustBe doc.version
      md.isFolder mustBe Some(false)
      md.path mustBe doc.path
      md.description mustBe doc.description
      md.lock mustBe doc.lock

      val symExtAttr = md.extraAttributes.value
      symExtAttr.getAs[Boolean]("published") mustBe Some(doc.published)
      symExtAttr.getAs[String]("documentMedium") mustBe doc.documentMedium
      symExtAttr.getAs[Int]("documentNumber") mustBe Some(doc.documentDetails.number)
      symExtAttr.getAs[String]("documentType") mustBe doc.documentDetails.docType
      symExtAttr.getAs[String]("documentSubType") mustBe doc.documentDetails.docSubType
      symExtAttr.getAs[String]("author") mustBe doc.author
    }

    "be implicitly converted from a symbiotic File" in {
      val symFile: File       = doc
      val ad: ArchiveDocument = symFile

      ad mustBe doc
    }

  }

}
