package models.document

import models.document.ArchiveDocuments.ArchiveDocument
import models.document.ArchiveIdentifiers._
import models.document.Archiveables.DocumentDetails
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.PersistentType.UserStamp
import net.scalytica.symbiotic.api.types.ResourceOwner.{OrgOwner, Owner}
import net.scalytica.symbiotic.api.types.{File, FileId, Lock, Path}
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
        ownerType = OrgOwner
      )
    ),
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
      symFile.metadata.owner mustBe doc.owner
      symFile.metadata.fid mustBe doc.fid
      symFile.metadata.uploadedBy mustBe doc.createdStamp.map(_.by)
      symFile.metadata.version mustBe doc.version
      symFile.metadata.isFolder mustBe Some(false)
      symFile.metadata.path mustBe doc.path
      symFile.metadata.description mustBe doc.description
      symFile.metadata.lock mustBe doc.lock

      val symExtAttr = symFile.metadata.extraAttributes.value
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
